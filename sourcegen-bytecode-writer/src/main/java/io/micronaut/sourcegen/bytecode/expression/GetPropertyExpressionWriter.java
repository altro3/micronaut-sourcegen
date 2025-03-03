/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.bytecode.expression;

import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.JavaIdioms;
import org.objectweb.asm.commons.GeneratorAdapter;

final class GetPropertyExpressionWriter implements ExpressionWriter {
    private final ExpressionDef.GetPropertyValue getPropertyValue;

    public GetPropertyExpressionWriter(ExpressionDef.GetPropertyValue getPropertyValue) {
        this.getPropertyValue = getPropertyValue;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context) {
        ExpressionDef propertyValue = JavaIdioms.getPropertyValue(getPropertyValue);
        ExpressionWriter.writeExpressionCheckCast(generatorAdapter, context, propertyValue, getPropertyValue.type());
    }
}
