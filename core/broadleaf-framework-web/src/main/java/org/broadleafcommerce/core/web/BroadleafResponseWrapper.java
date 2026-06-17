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
package org.broadleafcommerce.core.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * @author jfischer
 *
 */
// TODO(java21-migration): The Servlet 6 (jakarta) HttpServletResponse SPI dropped the deprecated encodeUrl(String),
// encodeRedirectUrl(String) and setStatus(int, String) methods and added header-introspection methods. The previous
// hand-rolled "implements HttpServletResponse" delegate no longer compiles, so this now extends
// HttpServletResponseWrapper (which delegates every call to the wrapped response) and only overrides setStatus to keep
// tracking the last status code, preserving the original behavior.
public class BroadleafResponseWrapper extends HttpServletResponseWrapper {

    private int status;

    public BroadleafResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        super.setStatus(sc);
    }

}
