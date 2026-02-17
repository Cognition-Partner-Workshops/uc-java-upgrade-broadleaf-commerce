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

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.Map;

/**
 * In Thymeleaf 3, TemplateResolver was replaced by AbstractConfigurableTemplateResolver.
 * This resolver serves as a placeholder that can be configured via XML to support
 * database-backed template resolution.
 * 
 * @author Andre Azzolini (apazzolini)
 */
public class DatabaseTemplateResolver extends AbstractConfigurableTemplateResolver {

    @Override
    protected ITemplateResource computeTemplateResource(
            IEngineConfiguration configuration, String ownerTemplate,
            String template, String resourceName, String characterEncoding,
            Map<String, Object> templateResolutionAttributes) {
        return new StringTemplateResource(resourceName);
    }

}
