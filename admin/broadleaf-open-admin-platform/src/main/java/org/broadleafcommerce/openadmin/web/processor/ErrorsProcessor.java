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

package org.broadleafcommerce.openadmin.web.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.util.StringUtil;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.openadmin.web.form.entity.DynamicEntityFormInfo;
import org.broadleafcommerce.openadmin.web.form.entity.EntityForm;
import org.broadleafcommerce.openadmin.web.form.entity.Field;
import org.broadleafcommerce.openadmin.web.form.entity.Tab;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring5.context.IThymeleafBindStatus;
import org.thymeleaf.spring5.util.FieldUtils;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("blErrorsProcessor")
public class ErrorsProcessor extends AbstractAttributeTagProcessor {

    protected static final Log LOG = LogFactory.getLog(ErrorsProcessor.class);

    public static final String GENERAL_ERRORS_TAB_KEY = "generalErrors";
    public static final String GENERAL_ERROR_FIELD_KEY = "generalError";

    public ErrorsProcessor() {
        super(TemplateMode.HTML, "blc_admin", null, false, "errors", true, 10000, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                             String attributeValue, IElementTagStructureHandler structureHandler) {

        IThymeleafBindStatus bindStatus = FieldUtils.getBindStatus(context, attributeValue);

        if (bindStatus.isError()) {
            EntityForm form = (EntityForm) ((BindingResult) bindStatus.getErrors()).getTarget();

            Map<String, Map<String, List<String>>> result = new HashMap<String, Map<String, List<String>>>();
            for (FieldError err : bindStatus.getErrors().getFieldErrors()) {
                String tabName = EntityForm.DEFAULT_TAB_NAME;
                Tab tab = form.findTabForField(err.getField());
                if (tab != null) {
                    tabName = tab.getTitle();
                }

                Map<String, List<String>> tabErrors = result.get(tabName);
                if (tabErrors == null) {
                    tabErrors = new HashMap<String, List<String>>();
                    result.put(tabName, tabErrors);
                }
                if (err.getField().contains(DynamicEntityFormInfo.FIELD_SEPARATOR)) {
                    String fieldName = extractFieldName(err);
                    String[] fieldInfo = fieldName.split("\\" + DynamicEntityFormInfo.FIELD_SEPARATOR);
                    Field formField = form.getDynamicForm(fieldInfo[0]).getFields().get(fieldName);

                    if (formField != null) {
                        addFieldError(formField.getFriendlyName(), err.getCode(), tabErrors);
                    } else {
                        LOG.warn("Could not find field " + fieldName + " within the dynamic form " + fieldInfo[0]);
                        addFieldError(fieldName, err.getCode(), tabErrors);
                    }
                } else {
                    if (form.getTabs().size() > 0) {
                        Field formField = form.findField(err.getField());
                        if (formField != null) {
                            addFieldError(formField.getFriendlyName(), err.getCode(), tabErrors);
                        } else {
                            LOG.warn("Could not find field " + err.getField() + " within the main form");
                            addFieldError(err.getField(), err.getCode(), tabErrors);
                        }
                    } else {
                        structureHandler.setLocalVariable("tabErrors", tabErrors);
                        return;
                    }
                }
            }

            String translatedGeneralTab = GENERAL_ERRORS_TAB_KEY;
            BroadleafRequestContext blcContext = BroadleafRequestContext.getBroadleafRequestContext();
            if (blcContext != null && blcContext.getMessageSource() != null) {
                translatedGeneralTab = blcContext.getMessageSource().getMessage(translatedGeneralTab, null, translatedGeneralTab, blcContext.getJavaLocale());
            }

            for (ObjectError err : bindStatus.getErrors().getGlobalErrors()) {
                Map<String, List<String>> tabErrors = result.get(GENERAL_ERRORS_TAB_KEY);
                if (tabErrors == null) {
                    tabErrors = new HashMap<String, List<String>>();
                    result.put(translatedGeneralTab, tabErrors);
                }
                addFieldError(GENERAL_ERROR_FIELD_KEY, err.getCode(), tabErrors);
            }

            structureHandler.setLocalVariable("tabErrors", result);
        }
    }

    private String extractFieldName(FieldError err) {
        String fieldExpression = err.getField();
        String fieldName = StringUtil.extractFieldNameFromExpression(fieldExpression);
        return fieldName;
    }

    protected void addFieldError(String fieldName, String message, Map<String, List<String>> tabErrors) {
        List<String> messages = tabErrors.get(fieldName);
        if (messages == null) {
            messages = new ArrayList<String>();
            tabErrors.put(fieldName, messages);
        }
        messages.add(message);
    }

}
