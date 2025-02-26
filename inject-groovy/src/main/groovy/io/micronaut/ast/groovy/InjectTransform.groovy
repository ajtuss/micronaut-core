/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.ast.groovy

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import io.micronaut.ast.groovy.config.GroovyConfigurationMetadataBuilder
import io.micronaut.ast.groovy.utils.AstAnnotationUtils
import io.micronaut.ast.groovy.utils.AstMessageUtils
import io.micronaut.ast.groovy.utils.InMemoryByteCodeGroovyClassLoader
import io.micronaut.ast.groovy.utils.InMemoryClassWriterOutputVisitor
import io.micronaut.ast.groovy.visitor.GroovyPackageElement
import io.micronaut.ast.groovy.visitor.GroovyVisitorContext
import io.micronaut.context.annotation.Configuration
import io.micronaut.context.annotation.ConfigurationReader
import io.micronaut.context.annotation.Context
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.Nullable
import io.micronaut.inject.ast.Element
import io.micronaut.inject.configuration.ConfigurationMetadataBuilder
import io.micronaut.inject.writer.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.io.StringReaderSource
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier
import java.util.function.Predicate
/**
 * An AST transformation that produces metadata for use by the injection container
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
// IMPORTANT NOTE: This transform runs in phase CANONICALIZATION so it runs after TypeElementVisitorTransform
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class InjectTransform implements ASTTransformation, CompilationUnitAware {

    public static final String ANN_VALID = "javax.validation.Valid"
    public static final String ANN_CONSTRAINT = "javax.validation.Constraint"
    public static final String ANN_CONFIGURATION_ADVICE = "io.micronaut.runtime.context.env.ConfigurationAdvice"
    public static final String ANN_VALIDATED = "io.micronaut.validation.Validated"
    public static final Predicate<AnnotationMetadata> IS_CONSTRAINT = (Predicate<AnnotationMetadata>) { AnnotationMetadata am ->
        am.hasStereotype(InjectTransform.ANN_CONSTRAINT) || am.hasStereotype(InjectTransform.ANN_VALID)
    }
    CompilationUnit unit
    ConfigurationMetadataBuilder<ClassNode> configurationMetadataBuilder

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        configurationMetadataBuilder = new GroovyConfigurationMetadataBuilder(source, unit)
        ModuleNode moduleNode = source.getAST()
        Map<AnnotatedNode, BeanDefinitionVisitor> beanDefinitionWriters = [:]
        File classesDir = source.configuration.targetDirectory
        boolean defineClassesInMemory = source.classLoader instanceof InMemoryByteCodeGroovyClassLoader
        ClassWriterOutputVisitor outputVisitor
        if (defineClassesInMemory) {
            outputVisitor = new InMemoryClassWriterOutputVisitor(
                    source.classLoader as InMemoryByteCodeGroovyClassLoader
            )

        } else {
            outputVisitor = new DirectoryClassWriterOutputVisitor(
                    classesDir
            )
        }
        List<ClassNode> classes = moduleNode.getClasses()
        if (classes.size() == 1) {
            ClassNode classNode = classes[0]
            if (classNode.nameWithoutPackage == 'package-info') {
                PackageNode packageNode = classNode.getPackage()
                if (AstAnnotationUtils.hasStereotype(source, unit, packageNode, Configuration)) {
                    def annotationMetadata = AstAnnotationUtils.getAnnotationMetadata(source, unit, packageNode)
                    GroovyVisitorContext visitorContext = new GroovyVisitorContext(source, unit)
                    BeanConfigurationWriter writer = new BeanConfigurationWriter(
                            classNode.packageName,
                            new GroovyPackageElement(visitorContext, packageNode, annotationMetadata),
                            annotationMetadata
                    )
                    try {
                        writer.accept(outputVisitor)
                        outputVisitor.finish()
                    } catch (Throwable e) {
                        AstMessageUtils.error(source, classNode, "Error generating bean configuration for package-info class [${classNode.name}]: $e.message")
                    }
                }

                return
            }
        }

        for (ClassNode classNode in classes) {
            if ((classNode instanceof InnerClassNode && !Modifier.isStatic(classNode.getModifiers()))) {
                continue
            } else {
                if (classNode.isInterface()) {
                    if (AstAnnotationUtils.hasStereotype(source, unit, classNode, InjectVisitor.INTRODUCTION_TYPE) ||
                            AstAnnotationUtils.hasStereotype(source, unit, classNode, ConfigurationReader.class)) {
                        InjectVisitor injectVisitor = new InjectVisitor(source, unit, classNode, configurationMetadataBuilder)
                        injectVisitor.visitClass(classNode)
                        beanDefinitionWriters.putAll(injectVisitor.beanDefinitionWriters)
                    }
                } else {
                    InjectVisitor injectVisitor = new InjectVisitor(source, unit, classNode, configurationMetadataBuilder)
                    injectVisitor.visitClass(classNode)
                    beanDefinitionWriters.putAll(injectVisitor.beanDefinitionWriters)
                }
            }
        }

        for (entry in beanDefinitionWriters) {
            BeanDefinitionVisitor beanDefWriter = entry.value
            String beanTypeName = beanDefWriter.beanTypeName
            AnnotatedNode beanClassNode = entry.key
            try {
                BeanDefinitionReferenceWriter beanReferenceWriter = new BeanDefinitionReferenceWriter(
                        beanTypeName,
                        beanDefWriter
                )

                beanReferenceWriter.setRequiresMethodProcessing(beanDefWriter.requiresMethodProcessing())
                beanReferenceWriter.setContextScope(beanDefWriter.getAnnotationMetadata().hasDeclaredAnnotation(Context))
                beanDefWriter.visitBeanDefinitionEnd()
                if (classesDir != null) {
                    beanReferenceWriter.accept(outputVisitor)
                    beanDefWriter.accept(outputVisitor)
                } else if (source.source instanceof StringReaderSource && defineClassesInMemory) {
                    beanReferenceWriter.accept(outputVisitor)
                    beanDefWriter.accept(outputVisitor)

                }


            } catch (Throwable e) {
                AstMessageUtils.error(source, beanClassNode, "Error generating bean definition class for dependency injection of class [${beanTypeName}]: $e.message")
                e.printStackTrace(System.err)
            }
        }
        if (!beanDefinitionWriters.isEmpty()) {

            try {
                outputVisitor.finish()

            } catch (Throwable e) {
                AstMessageUtils.error(source, moduleNode, "Error generating META-INF/services files: $e.message")
                if (e.message == null) {
                    e.printStackTrace(System.err)
                }
            }
        }

        AstAnnotationUtils.invalidateCache()
    }

    @Override
    void setCompilationUnit(CompilationUnit unit) {
        this.unit = unit
    }

}
