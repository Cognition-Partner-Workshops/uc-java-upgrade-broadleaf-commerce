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

import org.apache.commons.io.IOUtils;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * A template resolver that delegates database-backed template lookups to a {@link DatabaseResourceResolver}.
 *
 * The {@link DatabaseResourceResolver} is injected in XML configuration via {@link #setResourceResolver(DatabaseResourceResolver)}.
 *
 * @author Andre Azzolini (apazzolini)
 */
// TODO(java21-migration): Thymeleaf 3 removed org.thymeleaf.templateresolver.TemplateResolver and the IResourceResolver
// SPI it depended on. Template resolvers now extend AbstractConfigurableTemplateResolver and return an ITemplateResource
// from computeTemplateResource(). The database lookup that previously happened through the injected IResourceResolver is
// performed here by reading the InputStream produced by DatabaseResourceResolver and wrapping it as a
// StringTemplateResource (returning null when the extension does not resolve the template, so other resolvers run).
public class DatabaseTemplateResolver extends AbstractConfigurableTemplateResolver {

    protected DatabaseResourceResolver resourceResolver;

    public DatabaseResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(DatabaseResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
            String template, String resourceName, String characterEncoding,
            Map<String, Object> templateResolutionAttributes) {
        if (resourceResolver == null) {
            return null;
        }
        InputStream is = resourceResolver.getResourceAsStream(resourceName);
        if (is == null) {
            return null;
        }
        try {
            Charset charset = (characterEncoding != null) ? Charset.forName(characterEncoding) : Charset.defaultCharset();
            String content = IOUtils.toString(is, charset);
            return new StringTemplateResource(content);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read database template resource '" + resourceName + "'", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
