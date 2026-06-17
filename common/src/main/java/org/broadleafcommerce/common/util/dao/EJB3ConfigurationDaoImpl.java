/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.common.util.dao;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Properties;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * 
 * @author jfischer
 *
 */
public class EJB3ConfigurationDaoImpl implements EJB3ConfigurationDao {

    private Metadata configuration = null;

    protected PersistenceUnitInfo persistenceUnitInfo;

    public Metadata getConfiguration() {
        synchronized(this) {
            if (configuration == null) {
                // TODO(java21-migration): Hibernate 6 removed org.hibernate.ejb.Ejb3Configuration; rebuild the
                // boot-time Metadata from the persistence unit's managed classes and properties. Callers that
                // previously used Configuration#getClassMapping(String) should use Metadata#getEntityBinding(String).
                Properties properties = new Properties();
                if (persistenceUnitInfo.getProperties() != null) {
                    properties.putAll(persistenceUnitInfo.getProperties());
                }
                properties.setProperty("hibernate.hbm2ddl.auto", "none");
                StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(properties)
                        .build();
                MetadataSources sources = new MetadataSources(serviceRegistry);
                if (persistenceUnitInfo.getManagedClassNames() != null) {
                    for (String managedClassName : persistenceUnitInfo.getManagedClassNames()) {
                        sources.addAnnotatedClassName(managedClassName);
                    }
                }
                configuration = sources.buildMetadata();
            }
        }
        return configuration;
    }

    public PersistenceUnitInfo getPersistenceUnitInfo() {
        return persistenceUnitInfo;
    }

    public void setPersistenceUnitInfo(PersistenceUnitInfo persistenceUnitInfo) {
        this.persistenceUnitInfo = persistenceUnitInfo;
    }
    
}
