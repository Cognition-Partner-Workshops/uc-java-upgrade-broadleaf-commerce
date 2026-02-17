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

package org.broadleafcommerce.common.web.payment.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.web.dialect.AbstractModelVariableModifierProcessor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component("blCreditCardTypesProcessor")
public class CreditCardTypesProcessor extends AbstractModelVariableModifierProcessor {

    protected static final Log LOG = LogFactory.getLog(CreditCardTypesProcessor.class);

    @Resource(name = "blCreditCardTypesExtensionManager")
    protected CreditCardTypesExtensionManager extensionManager;

    public CreditCardTypesProcessor() {
        super("credit_card_types");
    }

    @Override
    protected void modifyModelAttributes(ITemplateContext context, IProcessableElementTag element) {
        Map<String, String> creditCardTypes = new HashMap<String, String>();

        try {
            extensionManager.getProxy().populateCreditCardMap(creditCardTypes);
        } catch (Exception e) {
            LOG.warn("Unable to Populate Credit Card Types Map for this Payment Module, or card type is not needed.");
        }

        if (!creditCardTypes.isEmpty()) {
            addToModel(context, "paymentGatewayCardTypes", creditCardTypes);
        }
    }
}
