package com.broadleafcommerce.search.repository;

import com.broadleafcommerce.search.repository.entity.CategoryProductXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only repository for accessing category-product cross-reference data.
 * Used during indexing to build the full category hierarchy for each product.
 */
@Repository
public interface CategoryProductXrefRepository extends JpaRepository<CategoryProductXref, Long> {

    /**
     * Finds all category-product cross-references for a given product.
     */
    List<CategoryProductXref> findByProductId(Long productId);

    /**
     * Finds all category-product cross-references for a given category.
     */
    List<CategoryProductXref> findByCategoryId(Long categoryId);
}
