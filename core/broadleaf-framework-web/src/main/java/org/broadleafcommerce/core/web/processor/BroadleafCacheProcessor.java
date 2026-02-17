/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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

package org.broadleafcommerce.core.web.processor;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.config.service.SystemPropertiesService;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.web.service.SimpleCacheKeyResolver;
import org.broadleafcommerce.core.web.service.TemplateCacheKeyResolverService;
import org.springframework.web.context.request.WebRequest;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.Expression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import javax.annotation.Resource;

/**
 * Allows for a customizable cache mechanism that can be used to avoid expensive Thymeleaf processing for 
 * HTML fragments that are static.
 * 
 * @param cacheTimeout (optional) the maximum length of time that the fragment will be allowed to be cached.
 * @param cacheKey (optional) Thymeleaf expression that should contribute to the final cache key.
 *  
 * @author bpolster
 * @see {@link TemplateCacheKeyResolverService}
 * @see {@link SimpleCacheKeyResolver}
 */
public class BroadleafCacheProcessor extends AbstractAttributeTagProcessor {

    private static final Log LOG = LogFactory.getLog(BroadleafCacheProcessor.class);

    public static final String ATTR_NAME = "cache";

    protected Cache cache;

    @Resource(name = "blSystemPropertiesService")
    protected SystemPropertiesService systemPropertiesService;

    @Resource(name = "blTemplateCacheKeyResolver")
    protected TemplateCacheKeyResolverService cacheKeyResolver;

    public BroadleafCacheProcessor() {
        super(TemplateMode.HTML, "blc", null, false, ATTR_NAME, true, Integer.MIN_VALUE, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                             String attributeValue, IElementTagStructureHandler structureHandler) {
        if (shouldCache(context, attributeValue)) {
            String cacheKey = cacheKeyResolver.resolveCacheKey(context, tag);
            if (!StringUtils.isEmpty(cacheKey)) {
                net.sf.ehcache.Element cacheElement = getCache().get(cacheKey);
                if (cacheElement != null && !cacheElement.isExpired()) {
                    Object cachedValue = cacheElement.getObjectValue();
                    if (cachedValue instanceof String) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Template Cache Hit with cacheKey " + cacheKey);
                        }
                        structureHandler.replaceWith((String) cachedValue, false);
                        return;
                    }
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Template Cache Miss with cacheKey " + cacheKey);
                    }
                }
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Template not cached due to empty cacheKey");
                }
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Template caching disabled - not retrieving template from cache");
            }
        }
    }

    protected boolean shouldCache(ITemplateContext context, String cacheAttrValue) {
        if (StringUtils.isEmpty(cacheAttrValue)) {
            return false;
        }

        cacheAttrValue = cacheAttrValue.toLowerCase();
        if (!isCachingEnabled() || "false".equals(cacheAttrValue)) {
            return false;
        } else if ("true".equals(cacheAttrValue)) {
            return true;
        }

        Expression expression = (Expression) StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, cacheAttrValue);
        Object o = expression.execute(context);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            cacheAttrValue = (String) o;
            cacheAttrValue = cacheAttrValue.toLowerCase();
            return "true".equals(cacheAttrValue);
        }
        return false;
    }

    public Cache getCache() {
        if (cache == null) {
            cache = CacheManager.getInstance().getCache("blTemplateElements");
        }
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public boolean isCachingEnabled() {
        boolean enabled = !systemPropertiesService.resolveBooleanSystemProperty("disableThymeleafTemplateCaching");
        if (enabled) {
            BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();
            if (brc != null && brc.getWebRequest() != null) {
                WebRequest request = brc.getWebRequest();
                String disableCachingParam = request.getParameter("disableThymeleafTemplateCaching");
                if ("true".equals(disableCachingParam)) {
                    return false;
                }
            }
        }
        return enabled;
    }
}
