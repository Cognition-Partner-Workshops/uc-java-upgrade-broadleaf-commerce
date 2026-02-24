package com.broadleafcommerce.search.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Read-only JPA entity mapped to the BLC_SKU table in the monolith's catalog database.
 * Provides the microservice with direct read access to SKU data for indexing purposes.
 */
@Entity
@Table(name = "BLC_SKU")
public class CatalogSku {

    @Id
    @Column(name = "SKU_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LONG_DESCRIPTION")
    private String longDescription;

    @Column(name = "RETAIL_PRICE")
    private BigDecimal retailPrice;

    @Column(name = "SALE_PRICE")
    private BigDecimal salePrice;

    @Column(name = "ACTIVE_START_DATE")
    private Date activeStartDate;

    @Column(name = "ACTIVE_END_DATE")
    private Date activeEndDate;

    @Column(name = "URL_KEY")
    private String urlKey;

    @Column(name = "DEFAULT_PRODUCT_ID")
    private Long defaultProductId;

    @Column(name = "ADDL_PRODUCT_ID")
    private Long additionalProductId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Date getActiveStartDate() {
        return activeStartDate;
    }

    public void setActiveStartDate(Date activeStartDate) {
        this.activeStartDate = activeStartDate;
    }

    public Date getActiveEndDate() {
        return activeEndDate;
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
    }

    public String getUrlKey() {
        return urlKey;
    }

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }

    public Long getDefaultProductId() {
        return defaultProductId;
    }

    public void setDefaultProductId(Long defaultProductId) {
        this.defaultProductId = defaultProductId;
    }

    public Long getAdditionalProductId() {
        return additionalProductId;
    }

    public void setAdditionalProductId(Long additionalProductId) {
        this.additionalProductId = additionalProductId;
    }

    /**
     * Checks if this SKU is currently active based on start/end dates.
     */
    public boolean isActive() {
        Date now = new Date();
        if (activeStartDate == null) {
            return false;
        }
        if (now.before(activeStartDate)) {
            return false;
        }
        if (activeEndDate != null && now.after(activeEndDate)) {
            return false;
        }
        return true;
    }
}
