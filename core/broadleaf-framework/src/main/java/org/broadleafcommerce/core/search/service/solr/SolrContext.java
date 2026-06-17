/*
 * #%L
 * BroadleafCommerce Framework
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.core.search.service.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

/**
 * <p>
 * Provides a class that will statically hold the Solr server.
 * 
 * <p>
 * This is initialized in {@link SolrSearchServiceImpl} and used in {@link SolrIndexServiceImpl}
 * 
 * @author Andre Azzolini (apazzolini)
 */
public class SolrContext {

    public static final String PRIMARY = "primary";
    public static final String REINDEX = "reindex";

    protected static SolrClient adminServer = null;
    protected static SolrClient primaryServer = null;
    protected static SolrClient reindexServer = null;

    /**
     * Sets the primary SolrServer instance to communicate with Solr.  This is typically one of the following: 
     * <code>org.apache.solr.client.solrj.embedded.EmbeddedSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.HttpSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.LBHttpSolrServer</code>, 
     * or <code>org.apache.solr.client.solrj.impl.CloudSolrServer</code>
     * 
     * @param server
     */
    public static void setPrimaryServer(SolrClient server) {
        if (server != null && CloudSolrClient.class.isAssignableFrom(server.getClass())) {
            CloudSolrClient cs = (CloudSolrClient) server;
            // TODO(java21-migration): SolrJ 9 removed CloudSolrClient#getDefaultCollection(), so we can no
            // longer (a) read the configured collection to only default it when blank, nor (b) compare the
            // primary/reindex default collections to enforce uniqueness. We unconditionally default the
            // primary client to the PRIMARY collection (the Broadleaf convention), which also guarantees it
            // differs from the REINDEX collection set on the reindex client. Reinstate the original
            // configurability/validation once a Solr 9 mechanism to read the default collection is available.
            cs.setDefaultCollection(PRIMARY);

            if (reindexServer != null && server == reindexServer) {
                //If we already have a reindex server set, make sure it's not the same instance as the primary
                throw new IllegalArgumentException("The primary and reindex CloudSolrClients are the same instances. "
                        + "They must be different instances. Each instance must have a different defaultCollection or "
                        + "the defaultCollection must be unspecified and Broadleaf will set it.");
            }
        }

        primaryServer = server;
    }

    /**
     * Sets the SolrServer instance that points to the reindex core for the purpose of doing a full reindex, while the 
     * primary core is still serving serving requests.  This is typically one of the following: 
     * <code>org.apache.solr.client.solrj.embedded.EmbeddedSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.HttpSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.LBHttpSolrServer</code>, 
     * or <code>org.apache.solr.client.solrj.impl.CloudSolrServer</code>
     * 
     * @param server
     */
    public static void setReindexServer(SolrClient server) {
        if (server != null && CloudSolrClient.class.isAssignableFrom(server.getClass())) {
            CloudSolrClient cs = (CloudSolrClient) server;
            // TODO(java21-migration): see setPrimaryServer - SolrJ 9 removed CloudSolrClient#getDefaultCollection().
            // We unconditionally default the reindex client to the REINDEX collection, which guarantees it differs
            // from the PRIMARY collection set on the primary client. Restore the blank-check/uniqueness validation
            // once a Solr 9 mechanism to read the default collection is available.
            cs.setDefaultCollection(REINDEX);

            if (primaryServer != null && server == primaryServer) {
                //If we already have a primary server set, make sure it's not the same instance as the reindex
                throw new IllegalArgumentException("The primary and reindex CloudSolrClients are the same instances. "
                        + "They must be different instances. Each instance must have a different defaultCollection or "
                        + "the defaultCollection must be unspecified and Broadleaf will set it.");
            }
        }
        reindexServer = server;
    }

    /**
     * Sets the admin SolrServer instance to communicate with Solr for administrative reasons, like swapping cores. 
     * This is typically one of the following: 
     * <code>org.apache.solr.client.solrj.embedded.EmbeddedSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.HttpSolrServer</code>, 
     * <code>org.apache.solr.client.solrj.impl.LBHttpSolrServer</code>, 
     * or <code>org.apache.solr.client.solrj.impl.CloudSolrServer</code>
     * 
     * This should not typically need to be set unless using a stand-alone configuration, where the path to the 
     * /admin URI is different than the core URI.  This should not typically be set for EmbeddedSolrServer or 
     * CloudSolrServer.
     * 
     * @param server
     */
    public static void setAdminServer(SolrClient server) {
        adminServer = server;
    }

    /**
     * The adminServer is just a reference to a SolrServer component for connecting to Solr.  In newer 
     * versions of Solr, 4.4 and beyond, auto discovery of cores is  
     * provided.  When using a stand-alone server or server cluster, 
     * the admin server, for swapping cores, is a different URL. For example, 
     * one needs to use http://solrserver:8983/solr, which formerly acted as the admin server 
     * AND as the primary core.  As of Solr 4.4, one needs to specify the cores separately: 
     * http://solrserver:8983/solr/primary and http://solrserver:8983/solr/reindex, 
     * and use http://solrserver:8983/solr for swapping cores.
     * 
     * By default, this method attempts to return an admin server if configured. Otherwise, 
     * it returns the primary server if the admin server is null, which is backwards compatible 
     * with the way that BLC worked prior to this change.
     * 
     * @return
     */
    public static SolrClient getAdminServer() {
        if (adminServer != null) {
            return adminServer;
        }
        //If the admin server hasn't been set, return the primary server.
        return getServer();
    }

    /**
     * @return the primary Solr server
     */
    public static SolrClient getServer() {
        return primaryServer;
    }

    /**
     * @return the primary server if {@link #isSingleCoreMode()}, else the reindex server
     */
    public static SolrClient getReindexServer() {
        return isSingleCoreMode() ? primaryServer : reindexServer;
    }

    /**
     * @return if this Solr context has a reindex server set not
     */
    public static boolean isSingleCoreMode() {
        return reindexServer == null;
    }

    /**
     * Indicates if we are communicating with SolrCloud, rather than embedded, or Stand-alone 
     * Solr server(s).  This is important to know because Solr cloud is clustered, uses sharding, uses 
     * Zookeeper to manage state, and manages individual "collections" rather than "cores" for searching and reindexing.
     * 
     * @return
     */
    public static boolean isSolrCloudMode() {
        return CloudSolrClient.class.isAssignableFrom(getServer().getClass());
    }
}
