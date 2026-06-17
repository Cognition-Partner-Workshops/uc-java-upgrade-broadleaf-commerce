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
package org.broadleafcommerce.common.web.expression;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;

/**
 * Provides a skeleton to register multiple {@link BroadleafVariableExpression} implementors.
 *
 * @author Andre Azzolini (apazzolini)
 */
// TODO(java21-migration): Thymeleaf 3 removed the SpelVariableExpressionEvaluator extension point
// (org.thymeleaf.spring4.expression.SpelVariableExpressionEvaluator) and the computeAdditionalExpressionObjects() hook.
// Custom expression utility objects (e.g. #props, #theme) are now contributed through an IExpressionObjectFactory that
// the dialect exposes via IExpressionObjectDialect, so this class implements IExpressionObjectFactory and surfaces each
// registered BroadleafVariableExpression by its getName().
public class BroadleafVariableExpressionEvaluator implements IExpressionObjectFactory {

    @Resource(name = "blVariableExpressions")
    protected List<BroadleafVariableExpression> expressions = new ArrayList<BroadleafVariableExpression>();

    protected volatile Map<String, BroadleafVariableExpression> expressionsByName;

    protected Map<String, BroadleafVariableExpression> getExpressionsByName() {
        Map<String, BroadleafVariableExpression> map = expressionsByName;
        if (map == null) {
            map = new HashMap<String, BroadleafVariableExpression>();
            for (BroadleafVariableExpression expression : expressions) {
                if (!(expression instanceof NullBroadleafVariableExpression)) {
                    map.put(expression.getName(), expression);
                }
            }
            expressionsByName = map;
        }
        return map;
    }

    @Override
    public Set<String> getAllExpressionObjectNames() {
        return getExpressionsByName().keySet();
    }

    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
        return getExpressionsByName().get(expressionObjectName);
    }

    @Override
    public boolean isCacheable(String expressionObjectName) {
        return true;
    }

}
