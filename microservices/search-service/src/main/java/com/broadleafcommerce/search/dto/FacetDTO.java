package com.broadleafcommerce.search.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing a search facet and its available values/counts.
 * Mirrors SearchFacetDTO from the monolith.
 */
public class FacetDTO {

    private Long facetId;
    private String fieldName;
    private String label;
    private boolean active;
    private List<FacetValueDTO> values;

    public Long getFacetId() {
        return facetId;
    }

    public void setFacetId(Long facetId) {
        this.facetId = facetId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<FacetValueDTO> getValues() {
        return values;
    }

    public void setValues(List<FacetValueDTO> values) {
        this.values = values;
    }

    /**
     * Represents a single facet value with its count.
     */
    public static class FacetValueDTO {

        private String value;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private Long quantity;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public BigDecimal getMinValue() {
            return minValue;
        }

        public void setMinValue(BigDecimal minValue) {
            this.minValue = minValue;
        }

        public BigDecimal getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(BigDecimal maxValue) {
            this.maxValue = maxValue;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }
    }
}
