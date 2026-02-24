package com.broadleafcommerce.search.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Read-only JPA entity mapped to the BLC_PRODUCT table in the monolith's catalog database.
 * This entity provides the microservice with direct read access to product data
 * for indexing purposes, without requiring a separate catalog API.
 */
@Entity
@Table(name = "BLC_PRODUCT")
public class CatalogProduct {

    @Id
    @Column(name = "PRODUCT_ID")
    private Long id;

    @Column(name = "URL")
    private String url;

    @Column(name = "MANUFACTURE")
    private String manufacturer;

    @Column(name = "IS_FEATURED_PRODUCT")
    private Boolean isFeaturedProduct;

    @Column(name = "CAN_SELL_WITHOUT_OPTIONS")
    private Boolean canSellWithoutOptions;

    @Column(name = "DEFAULT_SKU_ID")
    private Long defaultSkuId;

    @Column(name = "DEFAULT_CATEGORY_ID")
    private Long defaultCategoryId;

    @Column(name = "ARCHIVED")
    private Character archived;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Boolean getIsFeaturedProduct() {
        return isFeaturedProduct;
    }

    public void setIsFeaturedProduct(Boolean isFeaturedProduct) {
        this.isFeaturedProduct = isFeaturedProduct;
    }

    public Boolean getCanSellWithoutOptions() {
        return canSellWithoutOptions;
    }

    public void setCanSellWithoutOptions(Boolean canSellWithoutOptions) {
        this.canSellWithoutOptions = canSellWithoutOptions;
    }

    public Long getDefaultSkuId() {
        return defaultSkuId;
    }

    public void setDefaultSkuId(Long defaultSkuId) {
        this.defaultSkuId = defaultSkuId;
    }

    public Long getDefaultCategoryId() {
        return defaultCategoryId;
    }

    public void setDefaultCategoryId(Long defaultCategoryId) {
        this.defaultCategoryId = defaultCategoryId;
    }

    public Character getArchived() {
        return archived;
    }

    public void setArchived(Character archived) {
        this.archived = archived;
    }
}
