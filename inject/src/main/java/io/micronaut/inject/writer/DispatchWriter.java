package io.micronaut.inject.writer;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.processing.JavaModelUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DispatchWriter extends AbstractClassFileWriter implements Opcodes {

    private static final Method DISPATCH_METHOD = new Method("dispatch", getMethodDescriptor(Object.class, Arrays.asList(int.class, Object.class, Object[].class)));

    private static final Method DISPATCH_ONE_METHOD = new Method("dispatchOne", getMethodDescriptor(Object.class, Arrays.asList(int.class, Object.class, Object.class)));

    private static final Method GET_TARGET_METHOD = new Method("getTargetMethodByIndex", getMethodDescriptor(java.lang.reflect.Method.class, Collections.singletonList(int.class)));

    private static final Method UNKNOWN_DISPATCH_AT_INDEX = new Method("unknownDispatchAtIndexException", getMethodDescriptor(RuntimeException.class, Collections.singletonList(int.class)));

    private static final String FIELD_INTERCEPTABLE = "$interceptable";

    private static final Type TYPE_REFLECTION_UTILS = Type.getType(ReflectionUtils.class);

    private static final org.objectweb.asm.commons.Method METHOD_GET_REQUIRED_METHOD = org.objectweb.asm.commons.Method.getMethod(
            ReflectionUtils.getRequiredInternalMethod(ReflectionUtils.class, "getRequiredMethod", Class.class, String.class, Class[].class));

    protected final Type thisType;
    protected boolean hasInterceptedMethod;
    public final List<DispatchTarget> dispatchTargets = new ArrayList<>();

    public DispatchWriter(Type thisType) {
        super();
        this.thisType = thisType;
    }

    public int addSetField(FieldElement beanField) {
        return addDispatchTarget(new FieldSetDispatchTarget(beanField));
    }

    public int addGetField(FieldElement beanField) {
        return addDispatchTarget(new FieldGetDispatchTarget(beanField));
    }

    public int addMethod(TypedElement declaringType, MethodElement methodElement) {
        return addMethod(declaringType, methodElement, false);
    }

    public int addMethod(TypedElement declaringType, MethodElement methodElement, boolean useOneDispatch) {
        return addDispatchTarget(new MethodDispatchTarget(declaringType, methodElement, useOneDispatch, !useOneDispatch));
    }

    public int addInterceptedMethod(TypedElement declaringType,
                                    MethodElement methodElement,
                                    String interceptedProxyClassName,
                                    String interceptedProxyBridgeMethodName) {
        hasInterceptedMethod = true;
        return addDispatchTarget(new InterceptableMethodDispatchTarget(declaringType, methodElement, interceptedProxyClassName, interceptedProxyBridgeMethodName, thisType));
    }

    public int addDispatchTarget(DispatchTarget dispatchTarget) {
        dispatchTargets.add(dispatchTarget);
        return dispatchTargets.size() - 1;
    }

    public void buildDispatchMethod(ClassWriter classWriter) {
        int[] cases = dispatchTargets.stream()
                .filter(DispatchTarget::supportsDispatchMulti)
                .mapToInt(dispatchTargets::indexOf)
                .toArray();
        if (cases.length == 0) {
            return;
        }
        GeneratorAdapter dispatchMethod = new GeneratorAdapter(classWriter.visitMethod(
                ACC_PROTECTED | Opcodes.ACC_FINAL,
                DISPATCH_METHOD.getName(),
                DISPATCH_METHOD.getDescriptor(),
                null,
                null),
                ACC_PROTECTED | Opcodes.ACC_FINAL,
                DISPATCH_METHOD.getName(),
                DISPATCH_METHOD.getDescriptor()
        );
        dispatchMethod.loadArg(0);
        dispatchMethod.tableSwitch(cases, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                DispatchTarget method = dispatchTargets.get(key);
                method.writeDispatchMulti(dispatchMethod);
                dispatchMethod.returnValue();
            }

            @Override
            public void generateDefault() {
                dispatchMethod.loadThis();
                dispatchMethod.loadArg(0);
                dispatchMethod.invokeVirtual(thisType, UNKNOWN_DISPATCH_AT_INDEX);
                dispatchMethod.throwException();
            }
        }, true);
        dispatchMethod.visitMaxs(DEFAULT_MAX_STACK, 1);
        dispatchMethod.visitEnd();
    }

    public void buildDispatchOneMethod(ClassWriter classWriter) {
        int[] cases = dispatchTargets.stream()
                .filter(DispatchTarget::supportsDispatchOne)
                .mapToInt(dispatchTargets::indexOf)
                .toArray();
        if (cases.length == 0) {
            return;
        }
        GeneratorAdapter dispatchMethod = new GeneratorAdapter(classWriter.visitMethod(
                ACC_PROTECTED | ACC_FINAL,
                DISPATCH_ONE_METHOD.getName(),
                DISPATCH_ONE_METHOD.getDescriptor(),
                null,
                null),
                ACC_PROTECTED | ACC_FINAL,
                DISPATCH_ONE_METHOD.getName(),
                DISPATCH_ONE_METHOD.getDescriptor()
        );
        dispatchMethod.loadArg(0);
        dispatchMethod.tableSwitch(cases, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                DispatchTarget method = dispatchTargets.get(key);
                method.writeDispatchOne(dispatchMethod);
                dispatchMethod.returnValue();
            }

            @Override
            public void generateDefault() {
                dispatchMethod.loadThis();
                dispatchMethod.loadArg(0);
                dispatchMethod.invokeVirtual(thisType, UNKNOWN_DISPATCH_AT_INDEX);
                dispatchMethod.throwException();
            }
        }, true);
        dispatchMethod.visitMaxs(DEFAULT_MAX_STACK, 1);
        dispatchMethod.visitEnd();
    }

    public void buildGetTargetMethodByIndex(ClassWriter classWriter) {
        GeneratorAdapter getTargetMethodByIndex = new GeneratorAdapter(classWriter.visitMethod(
                Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
                GET_TARGET_METHOD.getName(),
                GET_TARGET_METHOD.getDescriptor(),
                null,
                null),
                ACC_PROTECTED | Opcodes.ACC_FINAL,
                GET_TARGET_METHOD.getName(),
                GET_TARGET_METHOD.getDescriptor()
        );
        getTargetMethodByIndex.loadArg(0);
        int[] cases = dispatchTargets.stream()
                .filter(dispatchTarget -> dispatchTarget instanceof MethodDispatchTarget)
                .mapToInt(dispatchTargets::indexOf)
                .toArray();
        getTargetMethodByIndex.tableSwitch(cases, new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                MethodDispatchTarget method = (MethodDispatchTarget) dispatchTargets.get(key);
                Type declaringTypeObject = JavaModelUtils.getTypeReference(method.declaringType);
                List<ParameterElement> argumentTypes = Arrays.asList(method.methodElement.getSuspendParameters());

                getTargetMethodByIndex.push(declaringTypeObject);
                getTargetMethodByIndex.push(method.methodElement.getName());
                if (!argumentTypes.isEmpty()) {
                    int len = argumentTypes.size();
                    Iterator<ParameterElement> iter = argumentTypes.iterator();
                    pushNewArray(getTargetMethodByIndex, Class.class, len);
                    for (int i = 0; i < len; i++) {
                        ParameterElement type = iter.next();
                        pushStoreInArray(
                                getTargetMethodByIndex,
                                i,
                                len,
                                () -> getTargetMethodByIndex.push(JavaModelUtils.getTypeReference(type))
                        );

                    }
                } else {
                    getTargetMethodByIndex.getStatic(TYPE_REFLECTION_UTILS, "EMPTY_CLASS_ARRAY", Type.getType(Class[].class));
                }
                getTargetMethodByIndex.invokeStatic(TYPE_REFLECTION_UTILS, METHOD_GET_REQUIRED_METHOD);
                getTargetMethodByIndex.returnValue();
            }

            @Override
            public void generateDefault() {
                getTargetMethodByIndex.loadThis();
                getTargetMethodByIndex.loadArg(0);
                getTargetMethodByIndex.invokeVirtual(thisType, UNKNOWN_DISPATCH_AT_INDEX);
                getTargetMethodByIndex.throwException();
            }
        }, true);
        getTargetMethodByIndex.visitMaxs(DEFAULT_MAX_STACK, 1);
        getTargetMethodByIndex.visitEnd();
    }

    @Override
    public void accept(ClassWriterOutputVisitor classWriterOutputVisitor) throws IOException {
        throw new IllegalStateException();
    }

    @Internal
    public static class FieldGetDispatchTarget implements DispatchTarget {
        @NotNull
        final FieldElement beanField;

        public FieldGetDispatchTarget(FieldElement beanField) {
            this.beanField = beanField;
        }

        @Override
        public boolean supportsDispatchOne() {
            return true;
        }

        @Override
        public boolean supportsDispatchMulti() {
            return false;
        }

        @Override
        public void writeDispatchOne(GeneratorAdapter writer) {
            final Type propertyType = JavaModelUtils.getTypeReference(beanField.getType());
            final Type beanType = JavaModelUtils.getTypeReference(beanField.getOwningType());

            // load this
            writer.loadArg(1);
            pushCastToType(writer, beanType);

            // get field value
            writer.getField(
                    JavaModelUtils.getTypeReference(beanField.getOwningType()),
                    beanField.getName(),
                    propertyType);

            pushBoxPrimitiveIfNecessary(propertyType, writer);
        }

    }

    @Internal
    public static class FieldSetDispatchTarget implements DispatchTarget {
        @NotNull
        final FieldElement beanField;

        public FieldSetDispatchTarget(FieldElement beanField) {
            this.beanField = beanField;
        }

        @Override
        public boolean supportsDispatchOne() {
            return true;
        }

        @Override
        public boolean supportsDispatchMulti() {
            return false;
        }

        @Override
        public void writeDispatchOne(GeneratorAdapter writer) {
            final Type propertyType = JavaModelUtils.getTypeReference(beanField.getType());
            final Type beanType = JavaModelUtils.getTypeReference(beanField.getOwningType());

            // load this
            writer.loadArg(1);
            pushCastToType(writer, beanType);

            // load value
            writer.loadArg(2);
            pushCastToType(writer, propertyType);

            // get field value
            writer.putField(
                    beanType,
                    beanField.getName(),
                    propertyType);

            // push null return type
            writer.push((String) null);
        }
    }

    @Internal
    public static class MethodDispatchTarget implements DispatchTarget {
        public final TypedElement declaringType;
        public final MethodElement methodElement;
        public final boolean oneDispatch;
        public final boolean multiDispatch;

        private MethodDispatchTarget(TypedElement declaringType,
                                     MethodElement methodElement,
                                     boolean oneDispatch,
                                     boolean multiDispatch) {
            this.declaringType = declaringType;
            this.methodElement = methodElement;
            this.oneDispatch = oneDispatch;
            this.multiDispatch = multiDispatch;
        }

        @Override
        public boolean supportsDispatchOne() {
            return oneDispatch;
        }

        @Override
        public boolean supportsDispatchMulti() {
            return multiDispatch;
        }

        @Override
        public void writeDispatchMulti(GeneratorAdapter writer) {
            String methodName = methodElement.getName();

            List<ParameterElement> argumentTypes = Arrays.asList(methodElement.getSuspendParameters());
            Type declaringTypeObject = JavaModelUtils.getTypeReference(declaringType);

            ClassElement returnType = methodElement.isSuspend() ? ClassElement.of(Object.class) : methodElement.getReturnType();
            boolean isInterface = declaringType.getType().isInterface();
            Type returnTypeObject = JavaModelUtils.getTypeReference(returnType);

            // load this
            writer.loadArg(1);
            // duplicate target
            writer.dup();

            String methodDescriptor = getMethodDescriptor(returnType, argumentTypes);

            pushCastToType(writer, declaringTypeObject);
            boolean hasArgs = !argumentTypes.isEmpty();
            if (hasArgs) {
                int argCount = argumentTypes.size();
                Iterator<ParameterElement> argIterator = argumentTypes.iterator();
                for (int i = 0; i < argCount; i++) {
                    writer.loadArg(2);
                    writer.push(i);
                    writer.visitInsn(AALOAD);
                    // cast the return value to the correct type
                    pushCastToType(writer, argIterator.next());
                }
            }

            writer.visitMethodInsn(isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                    declaringTypeObject.getInternalName(), methodName,
                    methodDescriptor, isInterface);

            if (returnTypeObject.equals(Type.VOID_TYPE)) {
                writer.visitInsn(ACONST_NULL);
            } else {
                pushBoxPrimitiveIfNecessary(returnType, writer);
            }
        }

        @Override
        public void writeDispatchOne(GeneratorAdapter writer) {
            String methodName = methodElement.getName();

            List<ParameterElement> argumentTypes = Arrays.asList(methodElement.getSuspendParameters());
            Type declaringTypeObject = JavaModelUtils.getTypeReference(declaringType);

            ClassElement returnType = methodElement.isSuspend() ? ClassElement.of(Object.class) : methodElement.getReturnType();
            boolean isInterface = declaringType.getType().isInterface();
            Type returnTypeObject = JavaModelUtils.getTypeReference(returnType);

            writer.loadArg(1);
            pushCastToType(writer, declaringType);
            boolean hasArgs = !argumentTypes.isEmpty();
            if (hasArgs) {
                writer.loadArg(2);
                pushCastToType(writer, argumentTypes.get(0));
            }

            writer.visitMethodInsn(isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                    declaringTypeObject.getInternalName(), methodName,
                    getMethodDescriptor(returnType, argumentTypes), isInterface);

            if (returnTypeObject.equals(Type.VOID_TYPE)) {
                writer.visitInsn(ACONST_NULL);
            } else {
                pushBoxPrimitiveIfNecessary(returnType, writer);
            }
        }
    }

    @Internal
    public static final class InterceptableMethodDispatchTarget extends MethodDispatchTarget {
        final String interceptedProxyClassName;
        final String interceptedProxyBridgeMethodName;
        final Type thisType;

        private InterceptableMethodDispatchTarget(TypedElement declaringType,
                                                  MethodElement methodElement,
                                                  String interceptedProxyClassName,
                                                  String interceptedProxyBridgeMethodName,
                                                  Type thisType) {
            super(declaringType, methodElement, false, true);
            this.interceptedProxyClassName = interceptedProxyClassName;
            this.interceptedProxyBridgeMethodName = interceptedProxyBridgeMethodName;
            this.thisType = thisType;
        }

        public void writeDispatchMulti(GeneratorAdapter writer) {
            String methodName = methodElement.getName();

            List<ParameterElement> argumentTypes = Arrays.asList(methodElement.getSuspendParameters());
            Type declaringTypeObject = JavaModelUtils.getTypeReference(declaringType);

            ClassElement returnType = methodElement.isSuspend() ? ClassElement.of(Object.class) : methodElement.getReturnType();
            boolean isInterface = declaringType.getType().isInterface();
            Type returnTypeObject = JavaModelUtils.getTypeReference(returnType);

            // load this
            writer.loadArg(1);
            // duplicate target
            writer.dup();

            String methodDescriptor = getMethodDescriptor(returnType, argumentTypes);
            Label invokeTargetBlock = new Label();

            Type interceptedProxyType = getObjectType(interceptedProxyClassName);

            // load this.$interceptable field value
            writer.loadThis();
            writer.getField(thisType, FIELD_INTERCEPTABLE, Type.getType(boolean.class));
            // check if it equals true
            writer.push(true);
            writer.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, invokeTargetBlock);

            // target instanceOf intercepted proxy
            writer.loadArg(1);
            writer.instanceOf(interceptedProxyType);
            // check if instanceOf
            writer.push(true);
            writer.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, invokeTargetBlock);

            pushCastToType(writer, interceptedProxyType);

            // load arguments
            Iterator<ParameterElement> iterator = argumentTypes.iterator();
            for (int i = 0; i < argumentTypes.size(); i++) {
                writer.loadArg(2);
                writer.push(i);
                writer.visitInsn(AALOAD);

                pushCastToType(writer, iterator.next());
            }

            writer.visitMethodInsn(INVOKEVIRTUAL,
                    interceptedProxyType.getInternalName(), interceptedProxyBridgeMethodName,
                    methodDescriptor, false);

            if (returnTypeObject.equals(Type.VOID_TYPE)) {
                writer.visitInsn(ACONST_NULL);
            } else {
                pushBoxPrimitiveIfNecessary(returnType, writer);
            }
            writer.returnValue();

            writer.visitLabel(invokeTargetBlock);

            // remove parent
            writer.pop();

            pushCastToType(writer, declaringTypeObject);
            boolean hasArgs = !argumentTypes.isEmpty();
            if (hasArgs) {
                int argCount = argumentTypes.size();
                Iterator<ParameterElement> argIterator = argumentTypes.iterator();
                for (int i = 0; i < argCount; i++) {
                    writer.loadArg(2);
                    writer.push(i);
                    writer.visitInsn(AALOAD);
                    // cast the return value to the correct type
                    pushCastToType(writer, argIterator.next());
                }
            }

            writer.visitMethodInsn(isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                    declaringTypeObject.getInternalName(), methodName,
                    methodDescriptor, isInterface);

            if (returnTypeObject.equals(Type.VOID_TYPE)) {
                writer.visitInsn(ACONST_NULL);
            } else {
                pushBoxPrimitiveIfNecessary(returnType, writer);
            }
        }
    }

    public interface DispatchTarget {

        default boolean supportsDispatchOne() {
            return false;
        }

        default void writeDispatchOne(GeneratorAdapter writer) {
            throw new IllegalStateException("Not supported");
        }

        default boolean supportsDispatchMulti() {
            return false;
        }

        default void writeDispatchMulti(GeneratorAdapter writer) {
            throw new IllegalStateException("Not supported");
        }

    }

}
