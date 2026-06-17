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
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import jakarta.annotation.Resource;

/**
 * <p>
 * Allows for a customizable cache mechanism that can be used to avoid expensive Thymeleaf processing for 
 * HTML fragments that are static. For high volume sites, even a 30 second cache of pages can have significant overall
 * performance impacts.
 * 
 * <p>
 * When used as in conjunction within a {@code th:substituteby}, {@code th:replace} or {@code th:include} attribute this
 * will cache the template being included. If used in conjunction with {@link th:remove} this will cache all of the child
 * nodes of the element. If neither of these cases are true, this will cache the current node and all children.
 * 
 * <p>
 * The parameters allowed for this processor include a "cacheTimeout" and "cacheKey". This component will rely on
 * an implementation of {@link TemplateCacheKeyResolverService} to build the actual cacheKey used by the underlying
 * caching implementation.   The parameter named "cacheKey" will be used in the construction of the actual cacheKey
 * which may rely on variables like 
 * 
 * <p>
 * Implementors can create more functional cacheKey mechanisms. For example, Broadleaf Enterprise provides an 
 * additional implementation named {@code EnterpriseCacheKeyResolver} with support for additional caching 
 * features.
 * 
 * @param cacheTimeout (optional) the maximum length of time that the fragment will be allowed to be cached.   This
 * is important for fragments for which a good cacheKey would be difficult to generate.
 * @param cacheKey (optional) Thymeleaf expression that should contribute to the final cache key to reference the cached
 * element. The final key is determined by the {@link TemplateCacheKeyResolverService} but it is not required.
 * Implementations of {@link TemplateCacheKeyResolverService} can rely on variables like the customer, site, theme, etc. to
 * build the final cacheKey.
 *  
 * @author bpolster
 * @see {@link TemplateCacheKeyResolverService}
 * @see {@link SimpleCacheKeyResolver}
 */
public class BroadleafCacheProcessor extends AbstractAttributeTagProcessor {

    private static final Log LOG = LogFactory.getLog(BroadleafCacheProcessor.class);

    public static final String ATTR_NAME = "cache";
    private static final String DIALECT_PREFIX = "blc";

    protected Cache cache;

    @Resource(name = "blSystemPropertiesService")
    protected SystemPropertiesService systemPropertiesService;

    @Resource(name = "blTemplateCacheKeyResolver")
    protected TemplateCacheKeyResolverService cacheKeyResolver;

    public BroadleafCacheProcessor() {
        super(TemplateMode.HTML, DIALECT_PREFIX, null, false, ATTR_NAME, true, Integer.MIN_VALUE, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        // Always remove the blc:cache attribute so it does not leak into the rendered markup.
        structureHandler.removeAttribute(getDialectPrefix(), ATTR_NAME);

        // TODO(java21-migration): The Thymeleaf 2 implementation of this processor relied on the legacy DOM
        // model (org.thymeleaf.dom.Element / Attribute / ProcessorResult) to splice in wrapper elements,
        // clear children, recompute processors and short-circuit rendering with a cached fragment. Thymeleaf 3
        // replaced the mutable DOM with an event-based IModel/IModelFactory pipeline, which has no direct
        // equivalent for the in-place element rewriting this processor performed. The fragment-caching
        // behavior is therefore temporarily disabled (the attribute is simply stripped and the element is
        // rendered normally). The cache-key resolution and cache lookup infrastructure below is preserved so
        // a TL3-native re-implementation (using a post-processor / IModel manipulation) can be added later
        // without re-deriving the caching contract.
        if (shouldCache(context, tag, attributeValue) && LOG.isTraceEnabled()) {
            String cacheKey = checkCacheForElement(context, tag);
            LOG.trace("blc:cache attribute present (resolved cacheKey=" + cacheKey
                    + ") but fragment caching is currently a no-op pending Thymeleaf 3 model support.");
        }
    }

    protected boolean shouldCache(ITemplateContext context, IProcessableElementTag tag, String cacheAttrValue) {
        if (StringUtils.isEmpty(cacheAttrValue)) {
            return false;
        }

        cacheAttrValue = cacheAttrValue.toLowerCase();
        if (!isCachingEnabled() || "false".equals(cacheAttrValue)) {
            return false;
        } else if ("true".equals(cacheAttrValue)) {
            return true;
        }

        // Check for an expression
        IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
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

    /**
     * Resolves the cacheKey for the given element and, if found, returns the cache entry's key. Returns the
     * resolved cacheKey (or null) for diagnostic purposes.
     *
     * <p>TODO(java21-migration): in the original implementation this method mutated the legacy DOM element with
     * node properties (cacheKey / blCacheResponse) and signaled a cache hit so rendering could be short-circuited.
     * Those node-property hooks do not exist in Thymeleaf 3; only the cache lookup is retained.
     */
    protected String checkCacheForElement(ITemplateContext context, IProcessableElementTag tag) {
        if (isCachingEnabled()) {
            String cacheKey = cacheKeyResolver.resolveCacheKey(context, tag);

            if (!StringUtils.isEmpty(cacheKey)) {
                net.sf.ehcache.Element cacheElement = getCache().get(cacheKey);
                if (cacheElement != null && !checkExpired(tag, cacheElement)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Template Cache Hit with cacheKey " + cacheKey + " found in cache.");
                    }
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Template Cache Miss with cacheKey " + cacheKey + " not found in cache.");
                    }
                }
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Template not cached due to empty cacheKey");
                }
            }
            return cacheKey;
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Template caching disabled - not retrieving template from cache");
            }
        }
        return null;
    }

    /**
     * Returns true if the item has been 
     * @param tag
     * @param cacheElement
     * @return
     */
    protected boolean checkExpired(IProcessableElementTag tag, net.sf.ehcache.Element cacheElement) {
        if (cacheElement.isExpired()) {
            return true;
        } else {
            String cacheTimeout = tag.getAttributeValue("cacheTimeout");
            if (!StringUtils.isEmpty(cacheTimeout) && StringUtils.isNumeric(cacheTimeout)) {
                Long timeout = Long.valueOf(cacheTimeout) * 1000;
                Long expiryTime = cacheElement.getCreationTime() + timeout;
                if (expiryTime < System.currentTimeMillis()) {
                    return true;
                }
            }
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
            // check for a URL param that overrides caching - useful for testing if this processor is incorrectly
            // caching a page (possibly due to an bad cacheKey).

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
