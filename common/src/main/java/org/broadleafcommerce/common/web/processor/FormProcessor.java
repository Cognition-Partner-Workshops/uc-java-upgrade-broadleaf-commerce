/*
 * #%L
 * BroadleafCommerce Framework Web
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
package org.broadleafcommerce.common.web.processor;

import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.security.handler.CsrfFilter;
import org.broadleafcommerce.common.security.service.ExploitProtectionService;
import org.broadleafcommerce.common.security.service.StaleStateProtectionService;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Resource;

/**
 * Used as a replacement to the HTML {@code <form>} element which adds a CSRF token input field to forms that are submitted
 * via anything but GET. This is required to properly bypass the {@link CsrfFilter}.
 * 
 * @author apazzolini
 * @see {@link CsrfFilter}
 */
// TODO(java21-migration): Thymeleaf 3 removed the DOM model (org.thymeleaf.dom.Element) and AbstractElementProcessor.
// Renaming the host element while preserving its body now requires an element-model processor, so this extends
// AbstractElementModelProcessor: it rewrites the <blc:form> open/close tags to <form> and injects the CSRF/state hidden
// inputs into the event model. The TL2 setRecomputeProcessorsImmediately() hook no longer exists; the standard th:*
// processors continue to run on the rewritten <form> as part of normal model processing.
@Component("blFormProcessor")
public class FormProcessor extends AbstractElementModelProcessor {
    
    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService eps;

    @Resource(name = "blStaleStateProtectionService")
    protected StaleStateProtectionService spps;
    
    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public FormProcessor() {
        super(TemplateMode.HTML, "blc", "form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IProcessableElementTag formTag = (IProcessableElementTag) model.get(0);
        Map<String, String> attributes = new LinkedHashMap<String, String>(formTag.getAttributeMap());

        // If the form will be not be submitted with a GET, we must add the CSRF token
        // We do this instead of checking for a POST because post is default if nothing is specified
        if (!"GET".equalsIgnoreCase(attributes.get("method"))) {
            try {
                String csrfToken = eps.getCSRFToken();
                String stateVersionToken = null;
                if (spps.isEnabled()) {
                    stateVersionToken = spps.getStateVersionToken();
                }

                //detect multipart form
                if ("multipart/form-data".equalsIgnoreCase(attributes.get("enctype"))) {
                    IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                            .parseExpression(context, attributes.get("th:action"));
                    String action = (String) expression.execute(context);
                    String csrfQueryParameter = "?" + eps.getCsrfTokenParameter() + "=" + csrfToken;
                    if (stateVersionToken != null) {
                        csrfQueryParameter += "&" + spps.getStateVersionTokenParameter() + "=" + stateVersionToken;
                    }
                    attributes.remove("th:action");
                    attributes.put("action", action + csrfQueryParameter);
                } else {
                    model.insert(model.size() - 1, createHiddenInput(modelFactory, eps.getCsrfTokenParameter(), csrfToken));
                    if (stateVersionToken != null) {
                        model.insert(model.size() - 1, createHiddenInput(modelFactory, spps.getStateVersionTokenParameter(), stateVersionToken));
                    }
                }

            } catch (ServiceException e) {
                throw new RuntimeException("Could not get a CSRF token for this session", e);
            }
        }

        // Convert the <blc:form> node to a normal <form> node
        IOpenElementTag newOpen = modelFactory.createOpenElementTag("form", attributes, AttributeValueQuotes.DOUBLE, false);
        model.replace(0, newOpen);
        ICloseElementTag newClose = modelFactory.createCloseElementTag("form");
        model.replace(model.size() - 1, newClose);
    }

    protected IStandaloneElementTag createHiddenInput(IModelFactory modelFactory, String name, String value) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("type", "hidden");
        attributes.put("name", name);
        attributes.put("value", value);
        return modelFactory.createStandaloneElementTag("input", attributes, AttributeValueQuotes.DOUBLE, false, false);
    }

}
