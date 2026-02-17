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
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

@Component("blFormProcessor")
public class FormProcessor extends AbstractElementTagProcessor {
    
    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService eps;

    @Resource(name = "blStaleStateProtectionService")
    protected StaleStateProtectionService spps;
    
    public FormProcessor() {
        super(TemplateMode.HTML, "blc", "form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IModel model = modelFactory.createModel();

        Map<String, String> formAttrs = new HashMap<String, String>();
        for (String attrName : tag.getAttributeMap().keySet()) {
            if (!"blc:form".equalsIgnoreCase(attrName)) {
                formAttrs.put(attrName, tag.getAttributeValue(attrName));
            }
        }

        String method = tag.getAttributeValue("method");
        if (!"GET".equalsIgnoreCase(method)) {
            try {
                String csrfToken = eps.getCSRFToken();
                String stateVersionToken = null;
                if (spps.isEnabled()) {
                    stateVersionToken = spps.getStateVersionToken();
                }

                if ("multipart/form-data".equalsIgnoreCase(tag.getAttributeValue("enctype"))) {
                    String thAction = tag.getAttributeValue("th:action");
                    if (thAction != null) {
                        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
                        IStandardExpression expression = parser.parseExpression(context, thAction);
                        String action = (String) expression.execute(context);
                        String csrfQueryParameter = "?" + eps.getCsrfTokenParameter() + "=" + csrfToken;
                        if (stateVersionToken != null) {
                            csrfQueryParameter += "&" + spps.getStateVersionTokenParameter() + "=" + stateVersionToken;
                        }
                        formAttrs.remove("th:action");
                        formAttrs.put("action", action + csrfQueryParameter);
                    }
                }

                model.add(modelFactory.createOpenElementTag("form", formAttrs, null, false));

                if (!"multipart/form-data".equalsIgnoreCase(tag.getAttributeValue("enctype"))) {
                    Map<String, String> csrfAttrs = new HashMap<String, String>();
                    csrfAttrs.put("type", "hidden");
                    csrfAttrs.put("name", eps.getCsrfTokenParameter());
                    csrfAttrs.put("value", csrfToken);
                    model.add(modelFactory.createStandaloneElementTag("input", csrfAttrs, null, false, false));

                    if (stateVersionToken != null) {
                        Map<String, String> versionAttrs = new HashMap<String, String>();
                        versionAttrs.put("type", "hidden");
                        versionAttrs.put("name", spps.getStateVersionTokenParameter());
                        versionAttrs.put("value", stateVersionToken);
                        model.add(modelFactory.createStandaloneElementTag("input", versionAttrs, null, false, false));
                    }
                }
            } catch (ServiceException e) {
                throw new RuntimeException("Could not get a CSRF token for this session", e);
            }
        } else {
            model.add(modelFactory.createOpenElementTag("form", formAttrs, null, false));
        }

        structureHandler.replaceWith(model, true);
    }
    
}
