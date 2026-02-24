package com.broadleafcommerce.search.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Configures Solr client beans based on the deployment mode.
 *
 * Supports three modes mirroring the original SolrSearchServiceImpl constructors:
 * <ul>
 *   <li><b>embedded</b> - For local dev/testing (not recommended for production)</li>
 *   <li><b>standalone</b> - Single-node Solr instance</li>
 *   <li><b>cloud</b> - SolrCloud via ZooKeeper (primary target for scalable deployment)</li>
 * </ul>
 */
@Configuration
public class SolrClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SolrClientConfig.class);

    private SolrClient primarySolrClient;
    private SolrClient reindexSolrClient;
    private SolrClient adminSolrClient;

    @Bean(name = "primarySolrClient")
    public SolrClient primarySolrClient(SolrProperties props) {
        primarySolrClient = createSolrClient(props, props.getPrimaryName());
        LOG.info("Created primary SolrClient in '{}' mode for collection/core '{}'",
                props.getMode(), props.getPrimaryName());
        return primarySolrClient;
    }

    @Bean(name = "reindexSolrClient")
    public SolrClient reindexSolrClient(SolrProperties props) {
        reindexSolrClient = createSolrClient(props, props.getReindexName());
        LOG.info("Created reindex SolrClient in '{}' mode for collection/core '{}'",
                props.getMode(), props.getReindexName());
        return reindexSolrClient;
    }

    @Bean(name = "adminSolrClient")
    public SolrClient adminSolrClient(SolrProperties props) {
        // Admin client uses the base URL without a specific collection for administrative operations
        if ("cloud".equalsIgnoreCase(props.getMode())) {
            adminSolrClient = createCloudSolrClient(props, null);
        } else {
            adminSolrClient = new HttpSolrClient.Builder(props.getUrl()).build();
        }
        LOG.info("Created admin SolrClient in '{}' mode", props.getMode());
        return adminSolrClient;
    }

    private SolrClient createSolrClient(SolrProperties props, String collectionOrCore) {
        String mode = props.getMode();

        switch (mode.toLowerCase()) {
            case "cloud":
                return createCloudSolrClient(props, collectionOrCore);
            case "standalone":
                return createStandaloneSolrClient(props, collectionOrCore);
            case "embedded":
                LOG.warn("Embedded mode is intended for development/testing only. "
                        + "Using standalone client pointing to '{}/{}'", props.getUrl(), collectionOrCore);
                return createStandaloneSolrClient(props, collectionOrCore);
            default:
                throw new IllegalArgumentException(
                        "Unsupported Solr mode: '" + mode + "'. Supported modes: embedded, standalone, cloud");
        }
    }

    /**
     * Creates a CloudSolrClient for SolrCloud mode.
     * Mirrors the CloudSolrServer setup in the original SolrSearchServiceImpl.
     */
    private SolrClient createCloudSolrClient(SolrProperties props, String defaultCollection) {
        CloudSolrClient.Builder builder = new CloudSolrClient.Builder(
                Collections.singletonList(props.getCloud().getZkHost()),
                Optional.empty()
        );

        CloudSolrClient client = builder.build();

        if (defaultCollection != null && !defaultCollection.isEmpty()) {
            client.setDefaultCollection(defaultCollection);
        }

        LOG.info("Configured SolrCloud client with ZK host '{}', config name '{}', shards {}, collection '{}'",
                props.getCloud().getZkHost(),
                props.getCloud().getConfigName(),
                props.getCloud().getDefaultNumShards(),
                defaultCollection);

        return client;
    }

    /**
     * Creates an HttpSolrClient for standalone Solr mode.
     */
    private SolrClient createStandaloneSolrClient(SolrProperties props, String coreName) {
        String baseUrl = props.getUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String fullUrl = baseUrl + coreName;

        return new HttpSolrClient.Builder(fullUrl)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        closeSolrClient("primary", primarySolrClient);
        closeSolrClient("reindex", reindexSolrClient);
        closeSolrClient("admin", adminSolrClient);
    }

    private void closeSolrClient(String name, SolrClient client) {
        if (client != null) {
            try {
                client.close();
                LOG.info("Shut down {} SolrClient", name);
            } catch (IOException e) {
                LOG.error("Error shutting down {} SolrClient", name, e);
            }
        }
    }
}
