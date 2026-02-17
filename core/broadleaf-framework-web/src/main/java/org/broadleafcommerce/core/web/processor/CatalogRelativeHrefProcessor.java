/*
 * #%L
 * BroadleafCommerce CMS Module
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
package org.broadleafcommerce.core.web.processor;

import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogURLService;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.standard.expression.Expression;
import org.thymeleaf.standard.expression.StandardExpressions;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * For use with category and product entities.   Creates a relative URL using the
 * current URI appended with the url-key (or last fragment of the url).
 * 
 * Takes in a category or product object as a parameter.
 * 
 * Uses the current request for the baseURI.
 * 
 * This implementation will also a categoryId or productId to the end of the URL it generates.
 * 
 * @author bpolster
 */
public class CatalogRelativeHrefProcessor extends AbstractAttributeTagProcessor {

    private static final String RHREF = "rhref";
    private static final String HREF = "href";

    @Resource(name = "blCatalogURLService")
    protected CatalogURLService catalogURLService;

    public CatalogRelativeHrefProcessor() {
        super(TemplateMode.HTML, "blc", null, false, RHREF, true, 0, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                             String attributeValue, IElementTagStructureHandler structureHandler) {
        Expression expression = (Expression) StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, attributeValue);
        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();

        String relativeHref = buildRelativeHref(expression, context, request);

        structureHandler.setAttribute(HREF, relativeHref);
    }

    protected String buildRelativeHref(Expression expression, ITemplateContext context, HttpServletRequest request) {
        Object result = expression.execute(context);
        String currentUrl = request.getRequestURI();

        if (request.getQueryString() != null) {
            currentUrl = currentUrl + "?" + request.getQueryString();
        }

        if (result instanceof Product) {
            return catalogURLService.buildRelativeProductURL(currentUrl, (Product) result);
        } else if (result instanceof Category) {
            return catalogURLService.buildRelativeCategoryURL(currentUrl, (Category) result);
        }
        return "";
    }
}
