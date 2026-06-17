/*
 * #%L
 * BroadleafCommerce Integration
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
package org.broadleafcommerce.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Broadleaf-local replacement for Spring's
 * {@code org.springframework.test.context.transaction.TransactionConfiguration}, which was
 * deprecated in Spring 4.2 and removed in Spring 5. It is consumed by
 * {@link MergeTransactionalTestExecutionListener} to configure the bean name of the
 * transaction manager used to drive transactional integration tests and whether those
 * transactions should be rolled back by default.
 *
 * <p>TODO(java21-migration): legacy transactional test-harness shim retained to preserve the
 * existing rollback semantics of the Broadleaf integration tests. It can be removed if the
 * harness is reworked to use Spring's standard {@code @Transactional}/{@code @Rollback} support.</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransactionConfiguration {

    String transactionManager() default "transactionManager";

    boolean defaultRollback() default true;
}
