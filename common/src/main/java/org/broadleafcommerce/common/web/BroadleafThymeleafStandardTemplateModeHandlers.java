/*
 * #%L
 * BroadleafCommerce Common Libraries
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
package org.broadleafcommerce.common.web;

/**
 * Wrapped the set of standard Thymeleaf 2 template-mode handlers with caching-aware handlers.
 */
// TODO(java21-migration): Thymeleaf 3 removed org.thymeleaf.templatemode.StandardTemplateModeHandlers and the
// ITemplateModeHandler SPI entirely (template modes are now a fixed TemplateMode enum), so there are no standard
// handlers left to wrap. This class is retained only as a compiling placeholder for legacy Spring wiring.
public class BroadleafThymeleafStandardTemplateModeHandlers {

    public BroadleafThymeleafStandardTemplateModeHandlers() {
        // no-op
    }

}
