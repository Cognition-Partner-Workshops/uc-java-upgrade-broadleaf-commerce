package com.broadleafcommerce.search.repository.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Read-only JPA entity mapped to the BLC_FIELD table.
 * Represents a searchable/faceted field configuration used by both
 * the search and indexing services to determine which product/SKU
 * properties to index and make available for faceting.
 */
@Entity
@Table(name = "BLC_FIELD")
public class SearchField {

    @Id
    @Column(name = "FIELD_ID")
    private Long id;

    @Column(name = "ENTITY_TYPE")
    private String entityType;

    @Column(name = "PROPERTY_NAME")
    private String propertyName;

    @Column(name = "ABBREVIATION")
    private String abbreviation;

    @Column(name = "SEARCHABLE")
    private Boolean searchable;

    @Column(name = "FACET_FIELD_TYPE")
    private String facetFieldType;

    @Column(name = "TRANSLATABLE")
    private Boolean translatable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public String getFacetFieldType() {
        return facetFieldType;
    }

    public void setFacetFieldType(String facetFieldType) {
        this.facetFieldType = facetFieldType;
    }

    public Boolean getTranslatable() {
        return translatable;
    }

    public void setTranslatable(Boolean translatable) {
        this.translatable = translatable;
    }
}
