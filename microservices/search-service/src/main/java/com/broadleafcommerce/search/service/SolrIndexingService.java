package com.broadleafcommerce.search.service;

import com.broadleafcommerce.search.config.SolrProperties;
import com.broadleafcommerce.search.repository.CategoryProductXrefRepository;
import com.broadleafcommerce.search.repository.CategoryRepository;
import com.broadleafcommerce.search.repository.ProductRepository;
import com.broadleafcommerce.search.repository.SearchFieldRepository;
import com.broadleafcommerce.search.repository.SkuRepository;
import com.broadleafcommerce.search.repository.entity.CatalogCategory;
import com.broadleafcommerce.search.repository.entity.CatalogProduct;
import com.broadleafcommerce.search.repository.entity.CatalogSku;
import com.broadleafcommerce.search.repository.entity.CategoryProductXref;
import com.broadleafcommerce.search.repository.entity.SearchField;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service responsible for building and rebuilding the Solr index.
 *
 * This implementation mirrors the logic from SolrIndexServiceImpl in the monolith:
 * - Batch/page-based indexing approach (configurable page size)
 * - Dual-core swap strategy for zero-downtime reindexing
 * - Support for both product-based and SKU-based indexing
 * - Reads catalog data directly from the shared database via JPA repositories (read-only)
 * - ThreadLocal-based caching for per-thread catalog structure (container-safe)
 *
 * SolrCloud collection management (auto-creation, aliasing) mirrors the
 * afterPropertiesSet() logic in SolrSearchServiceImpl.
 */
@Service
public class SolrIndexingService {

    private static final Logger LOG = LoggerFactory.getLogger(SolrIndexingService.class);

    private static final String NAMESPACE_FIELD = "namespace";
    private static final String DEFAULT_NAMESPACE = "d";
    private static final String ID_FIELD = "id";
    private static final String PRODUCT_ID_FIELD = "productId";
    private static final String SKU_ID_FIELD = "skuId";
    private static final String CATEGORY_FIELD = "category";
    private static final String EXPLICIT_CATEGORY_FIELD = "explicitCategory";

    private final Object LOCK_OBJECT = new Object();
    private volatile boolean isLocked = false;

    private final SolrClient primarySolrClient;
    private final SolrClient reindexSolrClient;
    private final SolrClient adminSolrClient;
    private final SolrProperties solrProperties;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryProductXrefRepository categoryProductXrefRepository;
    private final SearchFieldRepository searchFieldRepository;

    /**
     * ThreadLocal-based catalog structure cache, mirroring the SolrIndexCachedOperation
     * pattern from the monolith. This is per-thread and container-safe.
     */
    private static final ThreadLocal<Map<Long, Set<Long>>> parentCategoriesByProductCache = new ThreadLocal<>();
    private static final ThreadLocal<Map<Long, Set<Long>>> parentCategoriesByCategoryCache = new ThreadLocal<>();

    public SolrIndexingService(
            @Qualifier("primarySolrClient") SolrClient primarySolrClient,
            @Qualifier("reindexSolrClient") SolrClient reindexSolrClient,
            @Qualifier("adminSolrClient") SolrClient adminSolrClient,
            SolrProperties solrProperties,
            ProductRepository productRepository,
            SkuRepository skuRepository,
            CategoryRepository categoryRepository,
            CategoryProductXrefRepository categoryProductXrefRepository,
            SearchFieldRepository searchFieldRepository) {
        this.primarySolrClient = primarySolrClient;
        this.reindexSolrClient = reindexSolrClient;
        this.adminSolrClient = adminSolrClient;
        this.solrProperties = solrProperties;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.categoryProductXrefRepository = categoryProductXrefRepository;
        this.searchFieldRepository = searchFieldRepository;
    }

    /**
     * Initializes SolrCloud collections if running in cloud mode.
     * Mirrors the afterPropertiesSet() logic in SolrSearchServiceImpl which
     * creates collections and aliases for the primary and reindex cores.
     */
    @PostConstruct
    public void initializeSolrCloud() {
        if (!"cloud".equalsIgnoreCase(solrProperties.getMode())) {
            LOG.info("Solr is not in cloud mode (mode={}). Skipping SolrCloud collection initialization.",
                    solrProperties.getMode());
            return;
        }

        LOG.info("Initializing SolrCloud collections...");

        try {
            if (primarySolrClient instanceof CloudSolrClient) {
                CloudSolrClient cloudClient = (CloudSolrClient) primarySolrClient;
                cloudClient.connect();

                Set<String> existingCollections = cloudClient.getClusterStateProvider().getLiveNodes() != null
                        ? new HashSet<>(CollectionAdminRequest.listCollections(cloudClient))
                        : new HashSet<>();

                ensureCollection(cloudClient, solrProperties.getPrimaryName(), existingCollections);
                ensureCollection(cloudClient, solrProperties.getReindexName(), existingCollections);

                LOG.info("SolrCloud collection initialization complete.");
            }
        } catch (Exception e) {
            LOG.warn("Could not initialize SolrCloud collections. They may need to be created manually. Error: {}",
                    e.getMessage());
        }
    }

    /**
     * Ensures a SolrCloud collection exists, creating it if necessary.
     * Mirrors the collection creation logic in SolrSearchServiceImpl.afterPropertiesSet().
     */
    private void ensureCollection(CloudSolrClient cloudClient, String collectionName,
                                  Set<String> existingCollections) throws SolrServerException, IOException {
        if (!existingCollections.contains(collectionName)) {
            LOG.info("Creating SolrCloud collection '{}' with {} shards and config '{}'",
                    collectionName,
                    solrProperties.getCloud().getDefaultNumShards(),
                    solrProperties.getCloud().getConfigName());
            try {
                CollectionAdminRequest.Create createRequest = CollectionAdminRequest.createCollection(
                        collectionName,
                        solrProperties.getCloud().getConfigName(),
                        solrProperties.getCloud().getDefaultNumShards(),
                        solrProperties.getCloud().getDefaultReplicationFactor()
                );
                createRequest.process(cloudClient);
                LOG.info("Successfully created SolrCloud collection '{}'", collectionName);
            } catch (Exception e) {
                LOG.warn("Could not create collection '{}': {}", collectionName, e.getMessage());
            }
        } else {
            LOG.info("SolrCloud collection '{}' already exists.", collectionName);
        }
    }

    /**
     * Checks if a reindex operation is currently in progress.
     * Mirrors SolrIndexServiceImpl.isReindexInProcess().
     */
    public boolean isReindexInProcess() {
        synchronized (LOCK_OBJECT) {
            return isLocked;
        }
    }

    /**
     * Rebuilds the entire Solr index using the dual-core swap strategy.
     *
     * Mirrors SolrIndexServiceImpl.rebuildIndex():
     * 1. Acquires the reindex lock (prevents concurrent reindexing)
     * 2. Deletes all documents from the reindex core
     * 3. Iterates through all products/SKUs in batches (page-based)
     * 4. Builds Solr documents and adds them to the reindex core
     * 5. Optimizes the reindex core
     * 6. Swaps the primary and reindex cores for zero-downtime deployment
     */
    public void rebuildIndex() throws IOException {
        synchronized (LOCK_OBJECT) {
            if (isLocked) {
                if (solrProperties.getIndex().isErrorOnConcurrentReIndex()) {
                    throw new IllegalStateException("More than one thread attempting to concurrently reindex Solr.");
                } else {
                    LOG.warn("Another thread is already reindexing. Skipping this request gracefully.");
                    return;
                }
            }
            isLocked = true;
        }

        long startTime = System.currentTimeMillis();
        try {
            LOG.info("Starting Solr index rebuild...");

            // Step 1: Delete all documents from the reindex core
            LOG.info("Deleting all documents from the reindex core");
            deleteAllReindexCoreDocuments();

            // Step 2: Build the index page by page
            int pageSize = solrProperties.getIndex().getPageSize();
            boolean useSku = solrProperties.getIndex().isUseSku();

            long totalItems;
            if (useSku) {
                totalItems = skuRepository.countAllActiveSkus();
            } else {
                totalItems = productRepository.countAllActiveProducts();
            }

            LOG.info("Total items to index: {}", totalItems);

            // Initialize per-thread cache (mirrors SolrIndexCachedOperation)
            try {
                parentCategoriesByProductCache.set(new HashMap<>());
                parentCategoriesByCategoryCache.set(new HashMap<>());

                int page = 0;
                while ((long) page * pageSize < totalItems) {
                    LOG.info("Building page number {}", page);
                    buildIncrementalIndex(page, pageSize, useSku);
                    page++;
                }
            } finally {
                parentCategoriesByProductCache.remove();
                parentCategoriesByCategoryCache.remove();
            }

            // Step 3: Optimize the reindex core
            LOG.info("Optimizing reindex core");
            try {
                reindexSolrClient.optimize();
            } catch (SolrServerException e) {
                LOG.warn("Could not optimize reindex core: {}", e.getMessage());
            }

            // Step 4: Swap cores for zero-downtime deployment
            swapActiveCores();

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("Finished rebuilding Solr index in {} ms", duration);
        } finally {
            synchronized (LOCK_OBJECT) {
                isLocked = false;
            }
        }
    }

    /**
     * Deletes all documents from the reindex core.
     * Mirrors SolrIndexServiceImpl.deleteAllReindexCoreDocuments().
     */
    private void deleteAllReindexCoreDocuments() throws IOException {
        try {
            String deleteQuery = NAMESPACE_FIELD + ":(\"" + DEFAULT_NAMESPACE + "\")";
            LOG.debug("Deleting by query: {}", deleteQuery);
            reindexSolrClient.deleteByQuery(deleteQuery);
            reindexSolrClient.commit();
        } catch (SolrServerException e) {
            throw new IOException("Could not delete documents from reindex core", e);
        }
    }

    /**
     * Builds an incremental index for a single page of products or SKUs.
     * Mirrors SolrIndexServiceImpl.buildIncrementalIndex().
     */
    private void buildIncrementalIndex(int page, int pageSize, boolean useSku) throws IOException {
        try {
            Collection<SolrInputDocument> documents = new ArrayList<>();

            if (useSku) {
                Page<CatalogSku> skuPage = skuRepository.findAllActiveSkus(PageRequest.of(page, pageSize));
                List<SearchField> fields = searchFieldRepository.findByEntityType("SKU");

                for (CatalogSku sku : skuPage.getContent()) {
                    SolrInputDocument doc = buildSkuDocument(sku, fields);
                    if (doc != null) {
                        documents.add(doc);
                    }
                }
            } else {
                Page<CatalogProduct> productPage = productRepository.findAllActiveProducts(
                        PageRequest.of(page, pageSize));
                List<SearchField> fields = searchFieldRepository.findByEntityType("PRODUCT");

                for (CatalogProduct product : productPage.getContent()) {
                    SolrInputDocument doc = buildProductDocument(product, fields);
                    if (doc != null) {
                        documents.add(doc);
                    }
                }
            }

            if (!documents.isEmpty()) {
                logDocuments(documents);
                reindexSolrClient.add(documents);
                commitToServer(reindexSolrClient);
            }

            LOG.debug("Indexed {} documents for page {}", documents.size(), page);
        } catch (SolrServerException e) {
            throw new IOException("Could not build incremental index for page " + page, e);
        }
    }

    /**
     * Builds a Solr document for a product.
     * Mirrors SolrIndexServiceImpl.buildDocument(Product, ...).
     */
    private SolrInputDocument buildProductDocument(CatalogProduct product, List<SearchField> fields) {
        SolrInputDocument document = new SolrInputDocument();

        // Add namespace and ID fields (mirrors attachBasicDocumentFields)
        document.addField(NAMESPACE_FIELD, DEFAULT_NAMESPACE);
        document.addField(ID_FIELD, DEFAULT_NAMESPACE + "_product_" + product.getId());
        document.addField(PRODUCT_ID_FIELD, product.getId());

        // Build category hierarchy
        attachCategoryFields(product.getId(), document);

        // Add searchable/facetable field values
        for (SearchField field : fields) {
            addFieldValueToDocument(document, product, field);
        }

        return document;
    }

    /**
     * Builds a Solr document for a SKU.
     * Mirrors SolrIndexServiceImpl.buildDocument(Sku, ...).
     */
    private SolrInputDocument buildSkuDocument(CatalogSku sku, List<SearchField> fields) {
        SolrInputDocument document = new SolrInputDocument();

        // Add namespace and ID fields (mirrors attachBasicDocumentFields)
        document.addField(NAMESPACE_FIELD, DEFAULT_NAMESPACE);
        document.addField(ID_FIELD, DEFAULT_NAMESPACE + "_sku_" + sku.getId());
        document.addField(SKU_ID_FIELD, sku.getId());

        // Build category hierarchy from the product associated with this SKU
        if (sku.getDefaultProductId() != null) {
            document.addField(PRODUCT_ID_FIELD, sku.getDefaultProductId());
            attachCategoryFields(sku.getDefaultProductId(), document);
        }

        // Add searchable/facetable field values
        for (SearchField field : fields) {
            addSkuFieldValueToDocument(document, sku, field);
        }

        return document;
    }

    /**
     * Attaches category and explicit category fields to the document.
     * Mirrors the category hierarchy building in SolrIndexServiceImpl.attachBasicDocumentFields()
     * and buildCategoryDocument() / buildFullCategoryHierarchy().
     */
    private void attachCategoryFields(Long productId, SolrInputDocument document) {
        // Get parent categories for this product
        Set<Long> parentCategories = getParentCategoriesForProduct(productId);

        Set<Long> allCategories = new HashSet<>();
        for (Long categoryId : parentCategories) {
            // Explicit category
            document.addField(EXPLICIT_CATEGORY_FIELD, categoryId);

            // Build full hierarchy
            buildFullCategoryHierarchy(categoryId, allCategories);
        }

        // Add all categories in the hierarchy
        for (Long categoryId : allCategories) {
            document.addField(CATEGORY_FIELD, categoryId);
        }
    }

    /**
     * Gets parent categories for a product, using the ThreadLocal cache.
     * Mirrors the CatalogStructure.parentCategoriesByProduct cache in the monolith.
     */
    private Set<Long> getParentCategoriesForProduct(Long productId) {
        Map<Long, Set<Long>> cache = parentCategoriesByProductCache.get();
        if (cache != null && cache.containsKey(productId)) {
            return cache.get(productId);
        }

        List<CategoryProductXref> xrefs = categoryProductXrefRepository.findByProductId(productId);
        Set<Long> categoryIds = new HashSet<>();
        for (CategoryProductXref xref : xrefs) {
            categoryIds.add(xref.getCategoryId());
        }

        if (cache != null) {
            cache.put(productId, categoryIds);
        }

        return categoryIds;
    }

    /**
     * Recursively builds the full category hierarchy.
     * Mirrors SolrIndexServiceImpl.buildFullCategoryHierarchy().
     */
    private void buildFullCategoryHierarchy(Long categoryId, Set<Long> indexedCategories) {
        if (indexedCategories.contains(categoryId)) {
            return;
        }
        indexedCategories.add(categoryId);

        Set<Long> parents = getParentCategoriesForCategory(categoryId);
        for (Long parentId : parents) {
            buildFullCategoryHierarchy(parentId, indexedCategories);
        }
    }

    /**
     * Gets parent categories for a category, using the ThreadLocal cache.
     */
    private Set<Long> getParentCategoriesForCategory(Long categoryId) {
        Map<Long, Set<Long>> cache = parentCategoriesByCategoryCache.get();
        if (cache != null && cache.containsKey(categoryId)) {
            return cache.get(categoryId);
        }

        Set<Long> parentIds = new HashSet<>();
        Optional<CatalogCategory> category = categoryRepository.findById(categoryId);
        if (category.isPresent() && category.get().getDefaultParentCategoryId() != null) {
            parentIds.add(category.get().getDefaultParentCategoryId());
        }

        if (cache != null) {
            cache.put(categoryId, parentIds);
        }

        return parentIds;
    }

    /**
     * Adds a field value to the Solr document based on the product entity and field configuration.
     * This is a simplified version of SolrIndexServiceImpl.getPropertyValues().
     */
    private void addFieldValueToDocument(SolrInputDocument document, CatalogProduct product, SearchField field) {
        String propertyName = field.getPropertyName();
        if (propertyName == null) {
            return;
        }

        Object value = resolveProductPropertyValue(product, propertyName);
        if (value == null) {
            return;
        }

        // Add searchable field
        if (Boolean.TRUE.equals(field.getSearchable()) && field.getAbbreviation() != null) {
            document.addField(field.getAbbreviation() + "_searchable", value);
        }

        // Add faceted field
        if (field.getFacetFieldType() != null && field.getAbbreviation() != null) {
            document.addField(field.getAbbreviation() + "_facet", value);
        }
    }

    /**
     * Adds a field value to the Solr document based on the SKU entity and field configuration.
     */
    private void addSkuFieldValueToDocument(SolrInputDocument document, CatalogSku sku, SearchField field) {
        String propertyName = field.getPropertyName();
        if (propertyName == null) {
            return;
        }

        Object value = resolveSkuPropertyValue(sku, propertyName);
        if (value == null) {
            return;
        }

        if (Boolean.TRUE.equals(field.getSearchable()) && field.getAbbreviation() != null) {
            document.addField(field.getAbbreviation() + "_searchable", value);
        }

        if (field.getFacetFieldType() != null && field.getAbbreviation() != null) {
            document.addField(field.getAbbreviation() + "_facet", value);
        }
    }

    /**
     * Resolves a property value from a CatalogProduct entity by property name.
     * Handles the common Broadleaf product properties.
     */
    private Object resolveProductPropertyValue(CatalogProduct product, String propertyName) {
        switch (propertyName) {
            case "manufacturer":
                return product.getManufacturer();
            case "url":
                return product.getUrl();
            case "defaultSku.name":
            case "defaultSku.description":
            case "defaultSku.longDescription":
            case "defaultSku.retailPrice":
            case "defaultSku.salePrice":
                // These reference the default SKU which would require a join.
                // For now, return null; a production implementation would resolve via SKU lookup.
                return null;
            default:
                return null;
        }
    }

    /**
     * Resolves a property value from a CatalogSku entity by property name.
     */
    private Object resolveSkuPropertyValue(CatalogSku sku, String propertyName) {
        switch (propertyName) {
            case "name":
                return sku.getName();
            case "description":
                return sku.getDescription();
            case "longDescription":
                return sku.getLongDescription();
            case "retailPrice":
                return sku.getRetailPrice();
            case "salePrice":
                return sku.getSalePrice();
            default:
                return null;
        }
    }

    /**
     * Swaps the primary and reindex cores/collections for zero-downtime deployment.
     * In SolrCloud mode, this uses collection aliases.
     * In standalone mode, this uses the CoreAdmin SWAP command.
     */
    private void swapActiveCores() throws IOException {
        try {
            if ("cloud".equalsIgnoreCase(solrProperties.getMode()) && adminSolrClient instanceof CloudSolrClient) {
                // SolrCloud: swap aliases
                LOG.info("Swapping SolrCloud collection aliases (primary <-> reindex)");

                CollectionAdminRequest.createAlias(
                        solrProperties.getPrimaryName() + "_temp",
                        solrProperties.getReindexName()
                ).process(adminSolrClient);

                CollectionAdminRequest.createAlias(
                        solrProperties.getReindexName(),
                        solrProperties.getPrimaryName()
                ).process(adminSolrClient);

                CollectionAdminRequest.createAlias(
                        solrProperties.getPrimaryName(),
                        solrProperties.getPrimaryName() + "_temp"
                ).process(adminSolrClient);

                // Clean up temp alias
                CollectionAdminRequest.deleteAlias(
                        solrProperties.getPrimaryName() + "_temp"
                ).process(adminSolrClient);
            } else {
                // Standalone: use CoreAdmin swap
                LOG.info("Swapping Solr cores (primary <-> reindex)");
                org.apache.solr.client.solrj.request.CoreAdminRequest.swapCore(
                        solrProperties.getPrimaryName(),
                        solrProperties.getReindexName(),
                        adminSolrClient
                );
            }
            LOG.info("Core/collection swap completed successfully");
        } catch (SolrServerException e) {
            throw new IOException("Could not swap Solr cores/collections", e);
        }
    }

    /**
     * Commits changes to the specified Solr server.
     * Mirrors SolrIndexServiceImpl.commit().
     */
    private void commitToServer(SolrClient server) throws IOException {
        if (!solrProperties.getIndex().isCommit()) {
            LOG.debug("The solr.index.commit property is false. Not committing. Ensure autoCommit is configured.");
            return;
        }

        try {
            LOG.debug("Committing changes to Solr index: softCommit={}, waitSearcher={}, waitFlush={}",
                    solrProperties.getIndex().isSoftCommit(),
                    solrProperties.getIndex().isWaitSearcher(),
                    solrProperties.getIndex().isWaitFlush());

            server.commit(
                    solrProperties.getIndex().isWaitFlush(),
                    solrProperties.getIndex().isWaitSearcher(),
                    solrProperties.getIndex().isSoftCommit()
            );
        } catch (SolrServerException e) {
            throw new IOException("Could not commit changes to Solr index", e);
        }
    }

    /**
     * Logs Solr documents at TRACE level.
     * Mirrors SolrIndexServiceImpl.logDocuments().
     */
    private void logDocuments(Collection<SolrInputDocument> documents) {
        if (LOG.isTraceEnabled()) {
            for (SolrInputDocument document : documents) {
                LOG.trace("Solr document: {}", document);
            }
        }
    }
}
