/*
 * #%L
 * BroadleafCommerce Common Libraries
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
package org.broadleafcommerce.common.extensibility.cache.ehcache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.broadleafcommerce.common.extensibility.context.ResourceInputStream;
import org.broadleafcommerce.common.extensibility.context.merge.MergeXmlConfigResource;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO(java21-migration): Spring 6 removed org.springframework.cache.ehcache.EhCacheManagerFactoryBean (Spring dropped
// EhCache 2.x integration). Re-implement the minimal FactoryBean lifecycle directly against the net.sf.ehcache API so
// the existing merged ehcache.xml configuration continues to bootstrap a net.sf.ehcache.CacheManager.
public class MergeEhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    protected Resource configLocation;

    protected CacheManager cacheManager;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @jakarta.annotation.Resource(name="blMergedCacheConfigLocations")
    protected Set<String> mergedCacheConfigLocations;

    protected List<Resource> configLocations;

    public void setConfigLocation(Resource configLocation) {
        this.configLocation = configLocation;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Configuration configuration;
        if (configLocation != null) {
            try (InputStream is = configLocation.getInputStream()) {
                configuration = ConfigurationFactory.parseConfiguration(is);
            }
        } else {
            configuration = ConfigurationFactory.parseConfiguration();
        }
        this.cacheManager = CacheManager.newInstance(configuration);
    }

    @Override
    public CacheManager getObject() {
        return this.cacheManager;
    }

    @Override
    public Class<?> getObjectType() {
        return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        if (this.cacheManager != null) {
            this.cacheManager.shutdown();
        }
        try {
            CacheManager cacheManager = getObject();
            if (cacheManager == null) {
                return;
            }
            Field cacheManagerTimer = CacheManager.class.getDeclaredField("cacheManagerTimer");
            cacheManagerTimer.setAccessible(true);
            Object failSafeTimer = cacheManagerTimer.get(cacheManager);
            Field timer = failSafeTimer.getClass().getDeclaredField("timer");
            timer.setAccessible(true);
            Object time = timer.get(failSafeTimer);
            Field thread = time.getClass().getDeclaredField("thread");
            thread.setAccessible(true);
            Thread item = (Thread) thread.get(time);
            item.setContextClassLoader(Thread.currentThread().getContextClassLoader().getParent());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void configureMergedItems() {
        List<Resource> temp = new ArrayList<Resource>();
        if (mergedCacheConfigLocations != null && !mergedCacheConfigLocations.isEmpty()) {
            for (String location : mergedCacheConfigLocations) {
                temp.add(applicationContext.getResource(location));
            }
        }
        if (configLocations != null && !configLocations.isEmpty()) {
            for (Resource resource : configLocations) {
                temp.add(resource);
            }
        }
        try {
            MergeXmlConfigResource merge = new MergeXmlConfigResource();
            ResourceInputStream[] sources = new ResourceInputStream[temp.size()];
            int j=0;
            for (Resource resource : temp) {
                sources[j] = new ResourceInputStream(resource.getInputStream(), resource.getURL().toString());
                j++;
            }
            setConfigLocation(merge.getMergedConfigResource(sources));
        } catch (Exception e) {
            throw new FatalBeanException("Unable to merge cache locations", e);
        }
    }

    public void setConfigLocations(List<Resource> configLocations) throws BeansException {
        this.configLocations = configLocations;
    }
}
