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

import io.micronaut.sourcegen.bytecode.AbstractConditionalWriter;
import io.micronaut.sourcegen.bytecode.MethodContext;
import io.micronaut.sourcegen.model.ExpressionDef;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;

final class IfElseExpressionWriter extends AbstractConditionalWriter implements ExpressionWriter {
    private final ExpressionDef.IfElse conditionIfElse;

    public IfElseExpressionWriter(ExpressionDef.IfElse conditionIfElse) {
        this.conditionIfElse = conditionIfElse;
    }

    @Override
    public void write(GeneratorAdapter generatorAdapter, MethodContext context) {
        Label elseLabel = new Label();
        pushElseConditionalExpression(generatorAdapter, context, conditionIfElse.condition(), elseLabel);
        Label end = new Label();
        ExpressionWriter.writeExpressionCheckCast(generatorAdapter, context, conditionIfElse.expression(), conditionIfElse.type());
        generatorAdapter.goTo(end);
        generatorAdapter.visitLabel(elseLabel);
        ExpressionWriter.writeExpressionCheckCast(generatorAdapter, context, conditionIfElse.elseExpression(), conditionIfElse.type());
        generatorAdapter.visitLabel(end);
    }
}
