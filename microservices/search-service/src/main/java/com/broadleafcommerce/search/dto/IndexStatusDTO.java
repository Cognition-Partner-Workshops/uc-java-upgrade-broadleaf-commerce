package com.broadleafcommerce.search.dto;

/**
 * DTO representing the current status of the indexing process.
 */
public class IndexStatusDTO {

    private boolean reindexInProgress;
    private String message;

    public IndexStatusDTO() {
    }

    public IndexStatusDTO(boolean reindexInProgress, String message) {
        this.reindexInProgress = reindexInProgress;
        this.message = message;
    }

    public boolean isReindexInProgress() {
        return reindexInProgress;
    }

    public void setReindexInProgress(boolean reindexInProgress) {
        this.reindexInProgress = reindexInProgress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
