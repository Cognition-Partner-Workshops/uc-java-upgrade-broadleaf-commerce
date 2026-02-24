package com.broadleafcommerce.search.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Read-only JPA entity mapped to the BLC_CATEGORY table in the monolith's catalog database.
 * Used by the indexer to resolve category hierarchies during product indexing.
 */
@Entity
@Table(name = "BLC_CATEGORY")
public class CatalogCategory {

    @Id
    @Column(name = "CATEGORY_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "URL")
    private String url;

    @Column(name = "URL_KEY")
    private String urlKey;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ACTIVE_START_DATE")
    private Date activeStartDate;

    @Column(name = "ACTIVE_END_DATE")
    private Date activeEndDate;

    @Column(name = "DEFAULT_PARENT_CATEGORY_ID")
    private Long defaultParentCategoryId;

    @Column(name = "ARCHIVED")
    private Character archived;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlKey() {
        return urlKey;
    }

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Long getDefaultParentCategoryId() {
        return defaultParentCategoryId;
    }

    public void setDefaultParentCategoryId(Long defaultParentCategoryId) {
        this.defaultParentCategoryId = defaultParentCategoryId;
    }

    public Character getArchived() {
        return archived;
    }

    public void setArchived(Character archived) {
        this.archived = archived;
    }
}
