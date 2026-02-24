package com.broadleafcommerce.search.repository;

import com.broadleafcommerce.search.repository.entity.CatalogSku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only repository for accessing SKU data from the monolith's catalog database.
 * Used by the indexing service to load SKUs for Solr indexing when useSku mode is enabled.
 */
@Repository
public interface SkuRepository extends JpaRepository<CatalogSku, Long> {

    /**
     * Finds all active SKUs with pagination.
     * Mirrors SkuDao.readAllActiveSkus(page, pageSize) from the monolith.
     */
    @Query("SELECT s FROM CatalogSku s WHERE s.activeStartDate <= CURRENT_TIMESTAMP "
            + "AND (s.activeEndDate IS NULL OR s.activeEndDate > CURRENT_TIMESTAMP)")
    Page<CatalogSku> findAllActiveSkus(Pageable pageable);

    /**
     * Counts all active SKUs.
     * Mirrors SkuDao.readCountAllActiveSkus() from the monolith.
     */
    @Query("SELECT COUNT(s) FROM CatalogSku s WHERE s.activeStartDate <= CURRENT_TIMESTAMP "
            + "AND (s.activeEndDate IS NULL OR s.activeEndDate > CURRENT_TIMESTAMP)")
    long countAllActiveSkus();

    /**
     * Finds SKUs by a list of IDs.
     */
    List<CatalogSku> findByIdIn(List<Long> ids);
}
