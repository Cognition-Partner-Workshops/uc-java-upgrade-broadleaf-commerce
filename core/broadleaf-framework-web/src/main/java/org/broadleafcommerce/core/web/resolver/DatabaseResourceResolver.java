/*
 * #%L
 * broadleaf-theme
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
package org.broadleafcommerce.core.web.resolver;

import org.broadleafcommerce.common.extension.ExtensionResultHolder;
import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.springframework.stereotype.Service;

import java.io.InputStream;

import jakarta.annotation.Resource;


/**
 * Provides an extension point for retrieving templates from the database.
 * 
 * @author Andre Azzolini (apazzolini)
 */
// TODO(java21-migration): Thymeleaf 3 removed the org.thymeleaf.resourceresolver.IResourceResolver SPI (and
// org.thymeleaf.TemplateProcessingParameters). Resource resolution is now driven by ITemplateResolver implementations
// that return an ITemplateResource. This bean is no longer a Thymeleaf IResourceResolver; it simply exposes the
// database-backed lookup (an InputStream of the resolved resource), and DatabaseTemplateResolver wraps the result into
// a Thymeleaf 3 ITemplateResource.
@Service("blDatabaseResourceResolver")
public class DatabaseResourceResolver {
    
    public String getName() {
        return "BL_DATABASE";
    }
    
    @Resource(name = "blDatabaseResourceResolverExtensionManager")
    protected DatabaseResourceResolverExtensionManager extensionManager;

    public InputStream getResourceAsStream(String resourceName) {
        ExtensionResultHolder erh = new ExtensionResultHolder();
        ExtensionResultStatusType result = extensionManager.getProxy().resolveResource(erh, resourceName);
        if (result ==  ExtensionResultStatusType.HANDLED) {
            return (InputStream) erh.getContextMap().get(DatabaseResourceResolverExtensionHandler.IS_KEY);
        }
        return null;
    }

}
