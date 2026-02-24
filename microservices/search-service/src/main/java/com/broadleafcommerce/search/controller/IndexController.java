package com.broadleafcommerce.search.controller;

import com.broadleafcommerce.search.dto.IndexStatusDTO;
import com.broadleafcommerce.search.service.SolrIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST controller exposing indexing operations as HTTP endpoints.
 *
 * These endpoints allow the monolith (or an admin tool) to trigger and monitor
 * reindexing operations remotely.
 *
 * Endpoint mapping to original SolrIndexService methods:
 * <ul>
 *   <li>POST /api/index/rebuild -> rebuildIndex</li>
 *   <li>GET  /api/index/status  -> isReindexInProcess</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/index")
public class IndexController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);

    private final SolrIndexingService indexingService;

    public IndexController(SolrIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * Triggers a full index rebuild using the dual-core swap strategy.
     * Mirrors SolrIndexService.rebuildIndex().
     *
     * This is an asynchronous-safe operation: if a rebuild is already in progress,
     * the request will be handled gracefully based on the errorOnConcurrentReIndex configuration.
     *
     * @return status indicating the rebuild was initiated
     */
    @PostMapping("/rebuild")
    public ResponseEntity<IndexStatusDTO> rebuildIndex() {
        LOG.info("Index rebuild requested via REST API");
        try {
            indexingService.rebuildIndex();
            return ResponseEntity.ok(
                    new IndexStatusDTO(false, "Index rebuild completed successfully"));
        } catch (IllegalStateException e) {
            LOG.warn("Concurrent reindex attempt: {}", e.getMessage());
            return ResponseEntity.status(409).body(
                    new IndexStatusDTO(true, "A reindex operation is already in progress"));
        } catch (IOException e) {
            LOG.error("Error during index rebuild", e);
            return ResponseEntity.internalServerError().body(
                    new IndexStatusDTO(false, "Index rebuild failed: " + e.getMessage()));
        }
    }

    /**
     * Returns the current indexing status.
     * Mirrors SolrIndexService.isReindexInProcess().
     *
     * @return current indexing status
     */
    @GetMapping("/status")
    public ResponseEntity<IndexStatusDTO> getIndexStatus() {
        boolean inProgress = indexingService.isReindexInProcess();
        String message = inProgress ? "Reindex is currently in progress" : "No reindex operation in progress";
        return ResponseEntity.ok(new IndexStatusDTO(inProgress, message));
    }
}
