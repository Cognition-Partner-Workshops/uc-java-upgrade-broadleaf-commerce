package com.broadleafcommerce.search.service;

import com.broadleafcommerce.search.config.SolrProperties;
import com.broadleafcommerce.search.dto.FacetDTO;
import com.broadleafcommerce.search.dto.ProductDTO;
import com.broadleafcommerce.search.dto.SearchRequestDTO;
import com.broadleafcommerce.search.dto.SearchResultDTO;
import com.broadleafcommerce.search.dto.SkuDTO;
import com.broadleafcommerce.search.repository.ProductRepository;
import com.broadleafcommerce.search.repository.SearchFieldRepository;
import com.broadleafcommerce.search.repository.SkuRepository;
import com.broadleafcommerce.search.repository.entity.CatalogProduct;
import com.broadleafcommerce.search.repository.entity.CatalogSku;
import com.broadleafcommerce.search.repository.entity.SearchField;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core search service that wraps the SearchService interface contract from the monolith.
 *
 * This service communicates with Apache Solr (supporting standalone and SolrCloud modes)
 * to execute search queries, retrieve faceted results, and return paginated product/SKU data.
 *
 * The implementation mirrors the logic in SolrSearchServiceImpl from the monolith:
 * - Builds SolrQuery objects with eDisMax query parser
 * - Supports faceted search with active facet filtering
 * - Supports category-scoped and query-based search
 * - Returns results with pagination metadata
 */
@Service
public class SolrSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);

    private static final String PRODUCT_ID_FIELD = "productId";
    private static final String SKU_ID_FIELD = "skuId";
    private static final String CATEGORY_FIELD = "category";
    private static final String EXPLICIT_CATEGORY_FIELD = "explicitCategory";
    private static final String NAMESPACE_FIELD = "namespace";
    private static final String DEFAULT_NAMESPACE = "d";

    private final SolrClient primarySolrClient;
    private final SolrProperties solrProperties;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final SearchFieldRepository searchFieldRepository;

    public SolrSearchService(
            @Qualifier("primarySolrClient") SolrClient primarySolrClient,
            SolrProperties solrProperties,
            ProductRepository productRepository,
            SkuRepository skuRepository,
            SearchFieldRepository searchFieldRepository) {
        this.primarySolrClient = primarySolrClient;
        this.solrProperties = solrProperties;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.searchFieldRepository = searchFieldRepository;
    }

    /**
     * Finds search results by free-text query.
     * Mirrors SearchService.findSearchResultsByQuery().
     */
    public SearchResultDTO findSearchResultsByQuery(SearchRequestDTO request) {
        String query = "(" + sanitizeQuery(request.getQuery()) + ")";
        return executeSearch(query, request, null, null);
    }

    /**
     * Finds search results scoped to a category (including sub-categories).
     * Mirrors SearchService.findSearchResultsByCategory().
     */
    public SearchResultDTO findSearchResultsByCategory(SearchRequestDTO request) {
        String filterQuery = CATEGORY_FIELD + ":\"" + request.getCategoryId() + "\"";
        String defaultSort = "category_sort_" + request.getCategoryId() + " asc";
        return executeSearch("*:*", request, defaultSort, filterQuery);
    }

    /**
     * Finds search results scoped to an explicit category (excluding sub-categories).
     * Mirrors SearchService.findExplicitSearchResultsByCategory().
     */
    public SearchResultDTO findExplicitSearchResultsByCategory(SearchRequestDTO request) {
        String filterQuery = EXPLICIT_CATEGORY_FIELD + ":\"" + request.getCategoryId() + "\"";
        String defaultSort = "category_sort_" + request.getCategoryId() + " asc";
        return executeSearch("*:*", request, defaultSort, filterQuery);
    }

    /**
     * Finds search results by both category and query.
     * Mirrors SearchService.findSearchResultsByCategoryAndQuery().
     */
    public SearchResultDTO findSearchResultsByCategoryAndQuery(SearchRequestDTO request) {
        String query = "(" + sanitizeQuery(request.getQuery()) + ")";
        String filterQuery = CATEGORY_FIELD + ":\"" + request.getCategoryId() + "\"";
        return executeSearch(query, request, null, filterQuery);
    }

    /**
     * Retrieves available search facets.
     * Mirrors SearchService.getSearchFacets().
     */
    public List<FacetDTO> getSearchFacets() {
        String entityType = solrProperties.getIndex().isUseSku() ? "SKU" : "PRODUCT";
        List<SearchField> fields = searchFieldRepository.findByEntityType(entityType);
        return buildFacetDTOs(fields);
    }

    /**
     * Retrieves available facets for a specific category.
     * Mirrors SearchService.getCategoryFacets().
     */
    public List<FacetDTO> getCategoryFacets(Long categoryId) {
        // For the microservice, we return all facets applicable to the entity type.
        // In the monolith, this would filter by category-specific facets via CategorySearchFacet.
        // This simplified approach returns all configured facets.
        String entityType = solrProperties.getIndex().isUseSku() ? "SKU" : "PRODUCT";
        List<SearchField> fields = searchFieldRepository.findByEntityType(entityType);
        return buildFacetDTOs(fields);
    }

    /**
     * Core search execution method mirroring SolrSearchServiceImpl.findSearchResults().
     *
     * Builds a SolrQuery with:
     * - eDisMax query parser for relevance-based searching
     * - Pagination (start, rows)
     * - Filter queries for category scoping and namespace isolation
     * - Sort clauses
     * - Facet configuration
     * - Active facet filter application
     */
    private SearchResultDTO executeSearch(String qualifiedSolrQuery, SearchRequestDTO request,
                                          String defaultSort, String filterQuery) {
        boolean useSku = solrProperties.getIndex().isUseSku();

        // Build the basic query - start cannot be negative (mirrors monolith logic)
        int start = (request.getPage() <= 0) ? 0 : (request.getPage() - 1);

        SolrQuery solrQuery = new SolrQuery()
                .setQuery(qualifiedSolrQuery)
                .setRows(request.getPageSize())
                .setStart(start * request.getPageSize());

        // Set the collection parameter for SolrCloud mode
        // This is ignored if not using SolrCloud (mirrors monolith behavior)
        solrQuery.setParam("collection", solrProperties.getPrimaryName());

        // Set the return field based on product or SKU mode
        if (useSku) {
            solrQuery.setFields(SKU_ID_FIELD);
        } else {
            solrQuery.setFields(PRODUCT_ID_FIELD);
        }

        // Apply filter queries
        if (filterQuery != null) {
            solrQuery.setFilterQueries(filterQuery);
        }

        // Add namespace filter for multi-tenant isolation
        solrQuery.addFilterQuery(NAMESPACE_FIELD + ":(\"" + DEFAULT_NAMESPACE + "\")");

        // Configure eDisMax query parser (mirrors monolith configuration)
        solrQuery.set("defType", "edismax");
        solrQuery.set("qf", buildQueryFieldsString(useSku));

        // Apply sort clause
        attachSortClause(solrQuery, request, defaultSort);

        // Apply active facet filters from the request
        attachActiveFacetFilters(solrQuery, request);

        // Configure facets on the query
        attachFacets(solrQuery, useSku);

        if (LOG.isTraceEnabled()) {
            try {
                LOG.trace("Solr query: {}", URLDecoder.decode(solrQuery.toString(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                LOG.trace("Couldn't URL decode Solr query: {}", solrQuery);
            }
        }

        // Execute the query against Solr
        QueryResponse response;
        int numResults;
        try {
            response = primarySolrClient.query(solrQuery);
            SolrDocumentList docs = response.getResults();
            numResults = (int) docs.getNumFound();

            if (LOG.isTraceEnabled()) {
                LOG.trace("Solr response: {}", response);
                for (SolrDocument doc : docs) {
                    LOG.trace("Document: {}", doc);
                }
            }
        } catch (SolrServerException | IOException e) {
            LOG.error("Error executing Solr search query", e);
            throw new RuntimeException("Could not perform search", e);
        }

        // Build the result DTO
        SearchResultDTO result = new SearchResultDTO();
        result.setPage(request.getPage());
        result.setPageSize(request.getPageSize());
        result.setTotalResults(numResults);

        // Extract facet results
        result.setFacets(extractFacetResults(response));

        // Retrieve the actual product/SKU entities from the database
        if (useSku) {
            List<SkuDTO> skus = getSkus(response.getResults());
            result.setSkus(skus);
        } else {
            List<ProductDTO> products = getProducts(response.getResults());
            result.setProducts(products);
        }

        return result;
    }

    /**
     * Builds the query fields string for eDisMax.
     * Mirrors SolrSearchServiceImpl.buildQueryFieldsString().
     */
    private String buildQueryFieldsString(boolean useSku) {
        String entityType = useSku ? "SKU" : "PRODUCT";
        List<SearchField> fields = searchFieldRepository.findByEntityTypeAndSearchableTrue(entityType);

        StringBuilder queryBuilder = new StringBuilder();
        for (SearchField field : fields) {
            if (field.getAbbreviation() != null) {
                queryBuilder.append(field.getAbbreviation()).append("_searchable ");
            }
        }

        // Default fallback if no searchable fields are configured
        if (queryBuilder.length() == 0) {
            queryBuilder.append("defaultSku.name_searchable ");
        }

        return queryBuilder.toString().trim();
    }

    /**
     * Attaches sort clause to the Solr query.
     * Mirrors SolrSearchServiceImpl.attachSortClause().
     */
    private void attachSortClause(SolrQuery query, SearchRequestDTO request, String defaultSort) {
        String sortQuery = request.getSortQuery();
        if (StringUtils.isNotBlank(sortQuery)) {
            String[] sortFields = sortQuery.split(",");
            for (String sortField : sortFields) {
                String[] parts = sortField.trim().split("\\s+");
                if (parts.length == 2) {
                    SolrQuery.ORDER order = "desc".equalsIgnoreCase(parts[1])
                            ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc;
                    query.addSort(parts[0], order);
                }
            }
        } else if (StringUtils.isNotBlank(defaultSort)) {
            String[] parts = defaultSort.trim().split("\\s+");
            if (parts.length == 2) {
                SolrQuery.ORDER order = "desc".equalsIgnoreCase(parts[1])
                        ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc;
                query.addSort(parts[0], order);
            }
        }
    }

    /**
     * Applies active facet filters from the search request.
     * Mirrors SolrSearchServiceImpl.attachActiveFacetFilters().
     */
    private void attachActiveFacetFilters(SolrQuery query, SearchRequestDTO request) {
        Map<String, String[]> filterCriteria = request.getFilterCriteria();
        if (filterCriteria != null) {
            for (Map.Entry<String, String[]> entry : filterCriteria.entrySet()) {
                String fieldName = entry.getKey();
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    StringBuilder filterBuilder = new StringBuilder();
                    filterBuilder.append("{!tag=").append(fieldName).append("}");
                    filterBuilder.append(fieldName).append(":(");
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) {
                            filterBuilder.append(" OR ");
                        }
                        filterBuilder.append("\"").append(scrubFacetValue(values[i])).append("\"");
                    }
                    filterBuilder.append(")");
                    query.addFilterQuery(filterBuilder.toString());
                }
            }
        }
    }

    /**
     * Configures facets on the Solr query.
     * Mirrors SolrSearchServiceImpl.attachFacets().
     */
    private void attachFacets(SolrQuery query, boolean useSku) {
        String entityType = useSku ? "SKU" : "PRODUCT";
        List<SearchField> fields = searchFieldRepository.findByEntityType(entityType);

        query.setFacet(true);
        query.setFacetMinCount(1);

        for (SearchField field : fields) {
            if (field.getFacetFieldType() != null && field.getAbbreviation() != null) {
                String facetFieldName = field.getAbbreviation() + "_facet";
                String taggedField = "{!ex=" + facetFieldName + "}" + facetFieldName;
                query.addFacetField(taggedField);
            }
        }
    }

    /**
     * Extracts facet results from the Solr response.
     */
    private List<FacetDTO> extractFacetResults(QueryResponse response) {
        List<FacetDTO> facets = new ArrayList<>();

        if (response.getFacetFields() != null) {
            for (FacetField facetField : response.getFacetFields()) {
                FacetDTO dto = new FacetDTO();
                dto.setFieldName(facetField.getName());
                dto.setLabel(facetField.getName());

                List<FacetDTO.FacetValueDTO> values = new ArrayList<>();
                if (facetField.getValues() != null) {
                    for (FacetField.Count count : facetField.getValues()) {
                        FacetDTO.FacetValueDTO valueDTO = new FacetDTO.FacetValueDTO();
                        valueDTO.setValue(count.getName());
                        valueDTO.setQuantity(count.getCount());
                        values.add(valueDTO);
                    }
                }
                dto.setValues(values);
                facets.add(dto);
            }
        }

        return facets;
    }

    /**
     * Retrieves Product DTOs from the database based on Solr response documents.
     * Mirrors SolrSearchServiceImpl.getProducts() - preserves sort order from Solr.
     */
    private List<ProductDTO> getProducts(SolrDocumentList documents) {
        List<Long> productIds = new ArrayList<>();
        for (SolrDocument doc : documents) {
            Object idValue = doc.getFieldValue(PRODUCT_ID_FIELD);
            if (idValue instanceof Long) {
                productIds.add((Long) idValue);
            } else if (idValue != null) {
                productIds.add(Long.parseLong(idValue.toString()));
            }
        }

        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CatalogProduct> products = productRepository.findByIdIn(productIds);

        // Build a map for O(1) lookup
        Map<Long, CatalogProduct> productMap = products.stream()
                .collect(Collectors.toMap(CatalogProduct::getId, p -> p, (a, b) -> a));

        // Preserve the order from Solr results (mirrors monolith sort-by-index behavior)
        List<ProductDTO> result = new ArrayList<>();
        for (Long id : productIds) {
            CatalogProduct product = productMap.get(id);
            if (product != null) {
                result.add(toProductDTO(product));
            }
        }

        return result;
    }

    /**
     * Retrieves SKU DTOs from the database based on Solr response documents.
     * Mirrors SolrSearchServiceImpl.getSkus() - preserves sort order from Solr.
     */
    private List<SkuDTO> getSkus(SolrDocumentList documents) {
        List<Long> skuIds = new ArrayList<>();
        for (SolrDocument doc : documents) {
            Object idValue = doc.getFieldValue(SKU_ID_FIELD);
            if (idValue instanceof Long) {
                skuIds.add((Long) idValue);
            } else if (idValue != null) {
                skuIds.add(Long.parseLong(idValue.toString()));
            }
        }

        if (skuIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CatalogSku> skus = skuRepository.findByIdIn(skuIds);

        Map<Long, CatalogSku> skuMap = skus.stream()
                .collect(Collectors.toMap(CatalogSku::getId, s -> s, (a, b) -> a));

        List<SkuDTO> result = new ArrayList<>();
        for (Long id : skuIds) {
            CatalogSku sku = skuMap.get(id);
            if (sku != null) {
                result.add(toSkuDTO(sku));
            }
        }

        return result;
    }

    /**
     * Converts a CatalogProduct entity to a ProductDTO.
     */
    private ProductDTO toProductDTO(CatalogProduct product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setUrl(product.getUrl());
        return dto;
    }

    /**
     * Converts a CatalogSku entity to a SkuDTO.
     */
    private SkuDTO toSkuDTO(CatalogSku sku) {
        SkuDTO dto = new SkuDTO();
        dto.setId(sku.getId());
        dto.setName(sku.getName());
        dto.setDescription(sku.getDescription());
        dto.setRetailPrice(sku.getRetailPrice());
        dto.setSalePrice(sku.getSalePrice());
        dto.setActive(sku.isActive());
        if (sku.getDefaultProductId() != null) {
            dto.setProductId(sku.getDefaultProductId());
        }
        return dto;
    }

    /**
     * Builds facet DTOs from search field configurations.
     */
    private List<FacetDTO> buildFacetDTOs(List<SearchField> fields) {
        List<FacetDTO> facets = new ArrayList<>();
        for (SearchField field : fields) {
            if (field.getFacetFieldType() != null) {
                FacetDTO dto = new FacetDTO();
                dto.setFacetId(field.getId());
                dto.setFieldName(field.getAbbreviation() != null
                        ? field.getAbbreviation() + "_facet" : field.getPropertyName());
                dto.setLabel(field.getPropertyName());
                facets.add(dto);
            }
        }
        return facets;
    }

    /**
     * Sanitizes the user's query string for safe Solr query construction.
     * Mirrors SolrSearchServiceImpl.sanitizeQuery() via SolrHelperService.
     * Removes/escapes characters that have special meaning in Solr query syntax.
     */
    private String sanitizeQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return "*:*";
        }

        // Replace HTML-encoded quotes with actual quotes
        query = query.replace("&quot;", "\"");

        // Escape Solr special characters except quotes (which are intentional for phrase search)
        String[] specialChars = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]",
                "^", "~", "*", "?", ":", "\\", "/"};
        for (String specialChar : specialChars) {
            query = query.replace(specialChar, "\\" + specialChar);
        }

        return query.trim();
    }

    /**
     * Scrubs a facet value for safe use in Solr queries.
     * Mirrors SolrHelperService.scrubFacetValue().
     */
    private String scrubFacetValue(String facetValue) {
        if (StringUtils.isBlank(facetValue)) {
            return facetValue;
        }
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}",
                "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            facetValue = facetValue.replace(specialChar, "\\" + specialChar);
        }
        return facetValue.trim();
    }
}
