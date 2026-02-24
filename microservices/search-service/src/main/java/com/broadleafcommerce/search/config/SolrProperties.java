package com.broadleafcommerce.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for Solr connectivity.
 * Supports three modes: embedded (for local dev/testing), standalone, and SolrCloud.
 *
 * All properties can be overridden via environment variables or application.properties.
 */
@Component
@ConfigurationProperties(prefix = "solr")
public class SolrProperties {

    /**
     * Solr deployment mode: "embedded", "standalone", or "cloud".
     */
    private String mode = "standalone";

    /**
     * URL for standalone Solr (e.g., "http://localhost:8983/solr").
     */
    private String url = "http://localhost:8983/solr";

    /**
     * Name of the primary Solr collection/core.
     */
    private String primaryName = "primary";

    /**
     * Name of the reindex Solr collection/core.
     */
    private String reindexName = "reindex";

    /**
     * Embedded Solr home directory path (used when mode=embedded).
     */
    private String embeddedHome = "";

    private Cloud cloud = new Cloud();

    private Index index = new Index();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public void setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
    }

    public String getReindexName() {
        return reindexName;
    }

    public void setReindexName(String reindexName) {
        this.reindexName = reindexName;
    }

    public String getEmbeddedHome() {
        return embeddedHome;
    }

    public void setEmbeddedHome(String embeddedHome) {
        this.embeddedHome = embeddedHome;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    /**
     * SolrCloud-specific configuration properties.
     */
    public static class Cloud {

        /**
         * ZooKeeper connection string for SolrCloud (e.g., "zk1:2181,zk2:2181,zk3:2181").
         */
        private String zkHost = "localhost:9983";

        /**
         * Name of the Solr configuration in ZooKeeper.
         * Mirrors the @Value("${solr.cloud.configName}") in SolrSearchServiceImpl.
         */
        private String configName = "blc";

        /**
         * Default number of shards for auto-created SolrCloud collections.
         * Mirrors the @Value("${solr.cloud.defaultNumShards}") in SolrSearchServiceImpl.
         */
        private int defaultNumShards = 2;

        /**
         * Default replication factor for auto-created SolrCloud collections.
         */
        private int defaultReplicationFactor = 1;

        public String getZkHost() {
            return zkHost;
        }

        public void setZkHost(String zkHost) {
            this.zkHost = zkHost;
        }

        public String getConfigName() {
            return configName;
        }

        public void setConfigName(String configName) {
            this.configName = configName;
        }

        public int getDefaultNumShards() {
            return defaultNumShards;
        }

        public void setDefaultNumShards(int defaultNumShards) {
            this.defaultNumShards = defaultNumShards;
        }

        public int getDefaultReplicationFactor() {
            return defaultReplicationFactor;
        }

        public void setDefaultReplicationFactor(int defaultReplicationFactor) {
            this.defaultReplicationFactor = defaultReplicationFactor;
        }
    }

    /**
     * Indexing-specific configuration properties.
     * Mirrors the @Value annotations in SolrIndexServiceImpl.
     */
    public static class Index {

        /**
         * Whether to use SKU-based indexing instead of product-based.
         */
        private boolean useSku = false;

        /**
         * Page size for batch indexing operations.
         */
        private int pageSize = 100;

        /**
         * Whether to commit after indexing operations.
         */
        private boolean commit = true;

        /**
         * Whether to use soft commit (more efficient, does not flush to disk).
         */
        private boolean softCommit = false;

        /**
         * Whether to wait for a new searcher to be configured after commit.
         */
        private boolean waitSearcher = true;

        /**
         * Whether to wait for flush to disk after commit.
         */
        private boolean waitFlush = true;

        /**
         * Whether to throw an error on concurrent reindex attempts.
         */
        private boolean errorOnConcurrentReIndex = false;

        public boolean isUseSku() {
            return useSku;
        }

        public void setUseSku(boolean useSku) {
            this.useSku = useSku;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public boolean isCommit() {
            return commit;
        }

        public void setCommit(boolean commit) {
            this.commit = commit;
        }

        public boolean isSoftCommit() {
            return softCommit;
        }

        public void setSoftCommit(boolean softCommit) {
            this.softCommit = softCommit;
        }

        public boolean isWaitSearcher() {
            return waitSearcher;
        }

        public void setWaitSearcher(boolean waitSearcher) {
            this.waitSearcher = waitSearcher;
        }

        public boolean isWaitFlush() {
            return waitFlush;
        }

        public void setWaitFlush(boolean waitFlush) {
            this.waitFlush = waitFlush;
        }

        public boolean isErrorOnConcurrentReIndex() {
            return errorOnConcurrentReIndex;
        }

        public void setErrorOnConcurrentReIndex(boolean errorOnConcurrentReIndex) {
            this.errorOnConcurrentReIndex = errorOnConcurrentReIndex;
        }
    }
}
