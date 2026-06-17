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

package org.thymeleaf.templatewriter;

/**
 * Wrapper for Thymeleaf's {@code AbstractGeneralTemplateWriter} that provided content caching on the node level.
 *
 * @author Andre Azzolini (apazzolini), Brian Polster (bpolster)
 */
// TODO(java21-migration): Thymeleaf 3 removed the entire template-writer SPI (org.thymeleaf.templatewriter.*,
// AbstractGeneralTemplateWriter) and the DOM model (org.thymeleaf.Arguments, org.thymeleaf.dom.Node/Element) that this
// node-level response cache relied on. Thymeleaf 3 performs its own template/expression caching via ICacheManager, so
// there is no longer a pluggable per-node writer to wrap. This class is retained only as a compiling placeholder for
// legacy Spring wiring; the caching behavior must be reimplemented against Thymeleaf 3's caching APIs if still required.
public class CacheAwareGeneralTemplateWriter {

    public CacheAwareGeneralTemplateWriter() {
        // no-op
    }

}
