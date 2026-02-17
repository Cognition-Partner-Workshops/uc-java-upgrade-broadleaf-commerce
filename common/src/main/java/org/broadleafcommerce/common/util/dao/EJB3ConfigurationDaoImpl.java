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
import org.hibernate.cfg.Configuration;

import java.util.Properties;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * 
 * @author jfischer
 *
 */
public class EJB3ConfigurationDaoImpl implements EJB3ConfigurationDao {

    private Configuration configuration = null;
    private Metadata metadata = null;

    protected PersistenceUnitInfo persistenceUnitInfo;

    private void initConfiguration() {
        synchronized(this) {
            if (configuration == null) {
                Configuration temp = new Configuration();
                String previousValue = persistenceUnitInfo.getProperties().getProperty("hibernate.hbm2ddl.auto");
                persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", "none");
                Properties props = persistenceUnitInfo.getProperties();
                for (String name : props.stringPropertyNames()) {
                    temp.setProperty(name, props.getProperty(name));
                }
                for (String className : persistenceUnitInfo.getManagedClassNames()) {
                    try {
                        temp.addAnnotatedClass(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(temp.getProperties())
                        .build();
                MetadataSources sources = new MetadataSources(serviceRegistry);
                for (String className : persistenceUnitInfo.getManagedClassNames()) {
                    try {
                        sources.addAnnotatedClass(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                metadata = sources.buildMetadata();
                temp.buildSessionFactory();
                if (previousValue != null) {
                    persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", previousValue);
                }
                configuration = temp;
            }
        }
    }

    public Configuration getConfiguration() {
        initConfiguration();
        return configuration;
    }

    public Metadata getMetadata() {
        initConfiguration();
        return metadata;
    }

    public PersistenceUnitInfo getPersistenceUnitInfo() {
        return persistenceUnitInfo;
    }

    public void setPersistenceUnitInfo(PersistenceUnitInfo persistenceUnitInfo) {
        this.persistenceUnitInfo = persistenceUnitInfo;
    }
    
}
