package com.broadleafcommerce.search.repository;

import com.broadleafcommerce.search.repository.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only repository for accessing product data from the monolith's catalog database.
 * Used by the indexing service to load products for Solr indexing.
 */
@Repository
public interface ProductRepository extends JpaRepository<CatalogProduct, Long> {

    /**
     * Finds all active (non-archived) products with pagination.
     * Mirrors ProductDao.readAllActiveProducts(page, pageSize) from the monolith.
     */
    @Query("SELECT p FROM CatalogProduct p WHERE (p.archived IS NULL OR p.archived = 'N')")
    Page<CatalogProduct> findAllActiveProducts(Pageable pageable);

    /**
     * Counts all active (non-archived) products.
     * Mirrors ProductDao.readCountAllActiveProducts() from the monolith.
     */
    @Query("SELECT COUNT(p) FROM CatalogProduct p WHERE (p.archived IS NULL OR p.archived = 'N')")
    long countAllActiveProducts();

    /**
     * Finds products by a list of IDs, preserving the order for search result rendering.
     */
    List<CatalogProduct> findByIdIn(List<Long> ids);
}
