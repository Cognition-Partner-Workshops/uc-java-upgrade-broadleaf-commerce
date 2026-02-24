package com.broadleafcommerce.search.repository;

import com.broadleafcommerce.search.repository.entity.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Read-only repository for accessing category data from the monolith's catalog database.
 * Used by the indexing service to resolve category hierarchies.
 */
@Repository
public interface CategoryRepository extends JpaRepository<CatalogCategory, Long> {
}
