/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.sourcegen.generator.visitors;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.SuperBuilder;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.TypeDef;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.micronaut.sourcegen.generator.visitors.BuilderAnnotationVisitor.addAnnotations;
import static io.micronaut.sourcegen.generator.visitors.BuilderAnnotationVisitor.createModifyPropertyMethod;

/**
 * The visitor that is generation a builder.
 *
 * @author Denis Stepanov
 * @since 1.2
 */
@Internal
public final class SuperBuilderAnnotationVisitor implements TypeElementVisitor<SuperBuilder, Object> {

    private final Set<String> processed = new HashSet<>();

    @Override
    public void start(VisitorContext visitorContext) {
        processed.clear();
    }

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (processed.contains(element.getName())) {
            return;
        }
        try {
            String abstractBuilderClassName = getAbstractSuperBuilderName(element);

            ClassTypeDef abstractBuilderType = ClassTypeDef.of(abstractBuilderClassName);

            TypeDef.TypeVariable selfType = TypeDef.variable("B");
            TypeDef.TypeVariable producingType = TypeDef.variable("C");
            ClassDef.ClassDefBuilder abstractBuilder = ClassDef.builder(abstractBuilderClassName)
                .addTypeVariable(TypeDef.variable("C", TypeDef.of(element)))
                .addTypeVariable(TypeDef.variable("B", TypeDef.parameterized(abstractBuilderType, producingType, selfType)))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

            ClassElement superType = element.getSuperType().orElse(null);
            if (superType != null && !superType.getName().equals("java.lang.Record")) {
                if (!superType.hasStereotype(SuperBuilder.class)) {
                    throw new ProcessingException(element, "Super type [" + superType.getName() + "] must be annotated with @" + SuperBuilder.class.getSimpleName());
                }
                String abstractSuperBuilderName = getAbstractSuperBuilderName(superType);
                abstractBuilder.superclass(TypeDef.parameterized(
                    ClassTypeDef.of(abstractSuperBuilderName),
                    List.of(
                        new TypeDef.TypeVariable("C"),
                        new TypeDef.TypeVariable("B")
                    )
                ));
            }

            List<PropertyElement> properties = element.getBeanProperties();
            for (PropertyElement beanProperty : properties) {
                if (!beanProperty.getDeclaringType().equals(element)) {
                    continue;
                }
                createModifyPropertyMethod(abstractBuilder, beanProperty, self -> self.invoke("self", self.type()).cast(selfType).returning());
            }

            abstractBuilder.addMethod(MethodDef.builder("self").addModifiers(Modifier.ABSTRACT).returns(selfType).build());
            abstractBuilder.addMethod(MethodDef.builder("build").addModifiers(Modifier.ABSTRACT).returns(producingType).build());

            ClassDef abstractBuilderDef = abstractBuilder.build();

            SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
            if (sourceGenerator == null) {
                return;
            }

            processed.add(element.getName());
            sourceGenerator.write(abstractBuilderDef, context, element);

            if (!element.isAbstract()) {

                String builderClassName = element.getPackageName() + "." + element.getSimpleName() + "SuperBuilder";

                ClassTypeDef builderType = ClassTypeDef.of(builderClassName);

                ClassDef.ClassDefBuilder builder = ClassDef.builder(builderClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(TypeDef.parameterized(
                            ClassTypeDef.of(abstractBuilderDef),
                            List.of(
                                ClassTypeDef.of(element),
                                builderType
                            )
                        )
                    );
                addAnnotations(builder, element.getAnnotation(SuperBuilder.class));

                builder.addMethod(MethodDef.constructor().build());
                if (!properties.isEmpty()) {
                    builder.addMethod(BuilderAnnotationVisitor.createAllPropertiesConstructor(properties));
                }

                builder.addMethod(createSelfMethod());
                builder.addMethod(BuilderAnnotationVisitor.createBuildMethod(element));
                builder.addMethod(createBuilderMethod(builderType));

                ClassDef builderDef = builder.build();
                sourceGenerator.write(builderDef, context, element);
            }
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            SourceGenerators.handleFatalException(
                element,
                SuperBuilder.class,
                e,
                (exception -> {
                    processed.remove(element.getName());
                    throw exception;
                })
            );
        }
    }

    private MethodDef createBuilderMethod(ClassTypeDef builderType) {
        return MethodDef.builder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderType)
            .addStatement(builderType.instantiate().returning())
            .build();
    }

    private MethodDef createSelfMethod() {
        return MethodDef.builder("self")
            .addModifiers(Modifier.PUBLIC)
            .build((self, parameterDefs) -> self.returning());
    }

    private String getAbstractSuperBuilderName(ClassElement element) {
        return element.getPackageName() + "." + "Abstract" + element.getSimpleName() + "SuperBuilder";
    }

}
