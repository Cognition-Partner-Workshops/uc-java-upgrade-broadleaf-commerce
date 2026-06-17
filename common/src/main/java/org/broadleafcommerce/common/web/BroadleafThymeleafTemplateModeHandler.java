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
package org.broadleafcommerce.common.web;

/**
 * Wrapped a Thymeleaf 2 {@code ITemplateModeHandler} so that its writer could be replaced with a caching writer.
 */
// TODO(java21-migration): Thymeleaf 3 removed the pluggable template-mode-handler SPI (org.thymeleaf.templatemode.
// ITemplateModeHandler, StandardTemplateModeHandlers) and the template-writer SPI it exposed. Template modes are now a
// fixed enum (org.thymeleaf.templatemode.TemplateMode) with no per-mode handler/writer customization, so this wrapper no
// longer has anything to wrap. It is retained only as a compiling placeholder for legacy Spring wiring.
public class BroadleafThymeleafTemplateModeHandler {

    public BroadleafThymeleafTemplateModeHandler() {
        // no-op
    }

}
