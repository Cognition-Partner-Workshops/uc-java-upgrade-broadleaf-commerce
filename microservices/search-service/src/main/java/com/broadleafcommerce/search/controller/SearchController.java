package com.broadleafcommerce.search.controller;

import com.broadleafcommerce.search.dto.FacetDTO;
import com.broadleafcommerce.search.dto.SearchRequestDTO;
import com.broadleafcommerce.search.dto.SearchResultDTO;
import com.broadleafcommerce.search.service.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the SearchService interface contract as HTTP endpoints.
 *
 * These endpoints allow the monolith (or any other client) to call the search service
 * remotely instead of using the in-process Spring bean.
 *
 * Endpoint mapping to original SearchService methods:
 * <ul>
 *   <li>POST /api/search/query            -> findSearchResultsByQuery</li>
 *   <li>POST /api/search/category         -> findSearchResultsByCategory</li>
 *   <li>POST /api/search/category/explicit -> findExplicitSearchResultsByCategory</li>
 *   <li>POST /api/search/category-and-query -> findSearchResultsByCategoryAndQuery</li>
 *   <li>GET  /api/search/facets           -> getSearchFacets</li>
 *   <li>GET  /api/search/facets/category/{categoryId} -> getCategoryFacets</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private final SolrSearchService searchService;

    public SearchController(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Performs a free-text search across all categories.
     * Mirrors SearchService.findSearchResultsByQuery().
     *
     * @param request the search request containing query, pagination, sort, and filter criteria
     * @return paginated search results with facets
     */
    @PostMapping("/query")
    public ResponseEntity<SearchResultDTO> findSearchResultsByQuery(@RequestBody SearchRequestDTO request) {
        LOG.debug("Search by query: '{}'", request.getQuery());
        SearchResultDTO result = searchService.findSearchResultsByQuery(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Performs a search scoped to a category (including sub-categories).
     * Mirrors SearchService.findSearchResultsByCategory().
     *
     * @param request the search request containing categoryId, pagination, sort, and filter criteria
     * @return paginated search results with facets
     */
    @PostMapping("/category")
    public ResponseEntity<SearchResultDTO> findSearchResultsByCategory(@RequestBody SearchRequestDTO request) {
        LOG.debug("Search by category: {}", request.getCategoryId());
        SearchResultDTO result = searchService.findSearchResultsByCategory(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Performs a search scoped to an explicit category (excluding sub-categories).
     * Mirrors SearchService.findExplicitSearchResultsByCategory().
     *
     * @param request the search request containing categoryId, pagination, sort, and filter criteria
     * @return paginated search results with facets
     */
    @PostMapping("/category/explicit")
    public ResponseEntity<SearchResultDTO> findExplicitSearchResultsByCategory(@RequestBody SearchRequestDTO request) {
        LOG.debug("Search by explicit category: {}", request.getCategoryId());
        SearchResultDTO result = searchService.findExplicitSearchResultsByCategory(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Performs a search by both category and free-text query.
     * Mirrors SearchService.findSearchResultsByCategoryAndQuery().
     *
     * @param request the search request containing categoryId, query, pagination, sort, and filter criteria
     * @return paginated search results with facets
     */
    @PostMapping("/category-and-query")
    public ResponseEntity<SearchResultDTO> findSearchResultsByCategoryAndQuery(@RequestBody SearchRequestDTO request) {
        LOG.debug("Search by category {} and query '{}'", request.getCategoryId(), request.getQuery());
        SearchResultDTO result = searchService.findSearchResultsByCategoryAndQuery(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns all available search facets.
     * Mirrors SearchService.getSearchFacets().
     *
     * @return list of available facets with their metadata
     */
    @GetMapping("/facets")
    public ResponseEntity<List<FacetDTO>> getSearchFacets() {
        LOG.debug("Getting search facets");
        List<FacetDTO> facets = searchService.getSearchFacets();
        return ResponseEntity.ok(facets);
    }

    /**
     * Returns available facets for a specific category.
     * Mirrors SearchService.getCategoryFacets().
     *
     * @param categoryId the category ID
     * @return list of available facets for the given category
     */
    @GetMapping("/facets/category/{categoryId}")
    public ResponseEntity<List<FacetDTO>> getCategoryFacets(@PathVariable Long categoryId) {
        LOG.debug("Getting category facets for category: {}", categoryId);
        List<FacetDTO> facets = searchService.getCategoryFacets(categoryId);
        return ResponseEntity.ok(facets);
    }
}
