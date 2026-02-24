package com.broadleafcommerce.search.repository;

import com.broadleafcommerce.search.repository.entity.SearchField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only repository for accessing search field configuration data.
 * Used to determine which product/SKU properties should be indexed and
 * made available as search facets.
 */
@Repository
public interface SearchFieldRepository extends JpaRepository<SearchField, Long> {

    /**
     * Finds all fields for a given entity type (e.g., "PRODUCT" or "SKU").
     * Mirrors FieldDao.readAllProductFields() / readAllSkuFields() from the monolith.
     */
    List<SearchField> findByEntityType(String entityType);

    /**
     * Finds all searchable fields for a given entity type.
     */
    List<SearchField> findByEntityTypeAndSearchableTrue(String entityType);
}
