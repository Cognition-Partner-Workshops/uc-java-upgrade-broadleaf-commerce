package com.broadleafcommerce.search.dto;

import java.util.List;

/**
 * DTO representing the search result returned to clients.
 * Mirrors the SearchResult class from the monolith but uses serializable DTOs
 * instead of Hibernate-managed domain entities.
 */
public class SearchResultDTO {

    private List<ProductDTO> products;
    private List<SkuDTO> skus;
    private List<FacetDTO> facets;
    private Integer totalResults;
    private Integer page;
    private Integer pageSize;

    public List<ProductDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDTO> products) {
        this.products = products;
    }

    public List<SkuDTO> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuDTO> skus) {
        this.skus = skus;
    }

    public List<FacetDTO> getFacets() {
        return facets;
    }

    public void setFacets(List<FacetDTO> facets) {
        this.facets = facets;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getStartResult() {
        if ((products == null || products.isEmpty()) && (skus == null || skus.isEmpty())) {
            return 0;
        }
        return ((page - 1) * pageSize) + 1;
    }

    public Integer getEndResult() {
        return Math.min(page * pageSize, totalResults);
    }

    public Integer getTotalPages() {
        if ((products == null || products.isEmpty()) && (skus == null || skus.isEmpty())) {
            return 1;
        }
        return (int) Math.ceil(totalResults * 1.0 / pageSize);
    }
}
