package com.broadleafcommerce.search.dto;

import java.util.Map;

/**
 * DTO representing a search request from the client.
 * Maps to the SearchCriteria and query parameters used in the monolith's SearchService interface.
 */
public class SearchRequestDTO {

    /**
     * The search query string (e.g., "blue shirt").
     */
    private String query;

    /**
     * Category ID to scope the search within.
     */
    private Long categoryId;

    /**
     * The page number (1-indexed).
     */
    private Integer page = 1;

    /**
     * The number of results per page.
     */
    private Integer pageSize = 15;

    /**
     * Sort query string (e.g., "price asc").
     */
    private String sortQuery;

    /**
     * Filter criteria keyed by facet field name, with an array of selected values.
     */
    private Map<String, String[]> filterCriteria;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getSortQuery() {
        return sortQuery;
    }

    public void setSortQuery(String sortQuery) {
        this.sortQuery = sortQuery;
    }

    public Map<String, String[]> getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(Map<String, String[]> filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
}
