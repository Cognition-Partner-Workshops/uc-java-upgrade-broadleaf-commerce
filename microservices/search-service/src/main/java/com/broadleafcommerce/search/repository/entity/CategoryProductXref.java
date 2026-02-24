package com.broadleafcommerce.search.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Read-only JPA entity mapped to the BLC_CATEGORY_PRODUCT_XREF table.
 * Represents the many-to-many relationship between categories and products,
 * including display order for category-scoped sorting.
 */
@Entity
@Table(name = "BLC_CATEGORY_PRODUCT_XREF")
public class CategoryProductXref {

    @Id
    @Column(name = "CATEGORY_PRODUCT_ID")
    private Long id;

    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "DISPLAY_ORDER")
    private BigDecimal displayOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(BigDecimal displayOrder) {
        this.displayOrder = displayOrder;
    }
}
