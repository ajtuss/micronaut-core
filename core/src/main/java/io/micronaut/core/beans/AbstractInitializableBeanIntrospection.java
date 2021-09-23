package io.micronaut.core.beans;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.UsedByGeneratedCode;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.reflect.exception.InstantiationException;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.core.util.ArgumentUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractInitializableBeanIntrospection<B> implements BeanIntrospection<B> {

    private final Class<B> beanType;
    private final AnnotationMetadata annotationMetadata;
    private final AnnotationMetadata constructorAnnotationMetadata;
    private final Argument<?>[] constructorArguments;
    private final BeanProperty<B, Object>[] beanProperties;
    private final BeanMethod<B, Object>[] beanMethods;

    private BeanConstructor<B> beanConstructor;

    public AbstractInitializableBeanIntrospection(Class<B> beanType,
                                                  AnnotationMetadata annotationMetadata,
                                                  AnnotationMetadata constructorAnnotationMetadata,
                                                  Argument<?>[] constructorArguments,
                                                  BeanPropertyRef<Object>[] propertiesRefs,
                                                  BeanMethodRef<Object>[] methodsRefs) {
        this.beanType = beanType;
        this.annotationMetadata = annotationMetadata == null ? AnnotationMetadata.EMPTY_METADATA : annotationMetadata;
        this.constructorAnnotationMetadata = constructorAnnotationMetadata == null ? AnnotationMetadata.EMPTY_METADATA : constructorAnnotationMetadata;
        this.constructorArguments = constructorArguments == null ? Argument.ZERO_ARGUMENTS : constructorArguments;
        if (propertiesRefs != null) {
            BeanPropertyImpl<Object>[] beanProperties = new BeanPropertyImpl[propertiesRefs.length];
            for (int i = 0; i < propertiesRefs.length; i++) {
                beanProperties[i] = new BeanPropertyImpl<>(propertiesRefs[i]);
            }
            this.beanProperties = beanProperties;
        } else {
            this.beanProperties = new BeanProperty[0];
        }
        if (methodsRefs != null) {
            BeanMethodImpl<Object>[] beanMethods = new BeanMethodImpl[methodsRefs.length];
            for (int i = 0; i < methodsRefs.length; i++) {
                beanMethods[i] = new BeanMethodImpl<>(methodsRefs[i]);
            }
            this.beanMethods = beanMethods;
        } else {
            this.beanMethods = new BeanMethod[0];
        }
    }

    /**
     * Reflection free bean instantiation implementation for the given arguments.
     *
     * @param arguments The arguments
     * @return The bean
     */
    @NonNull
    @Internal
    @UsedByGeneratedCode
    protected abstract B instantiateInternal(@Nullable Object[] arguments);

    @Nullable
    @Internal
    @UsedByGeneratedCode
    protected BeanProperty<B, Object> findProperty(@NonNull String name) {
        return null;
    }

    @Internal
    @UsedByGeneratedCode
    protected BeanProperty<B, Object> getPropertyByIndex(int index) {
        return beanProperties[index];
    }

    /**
     * Triggers the invocation of the method at index.
     *
     * @param index  The method index
     * @param target The target
     * @param args   The arguments
     * @return The result
     */
    @Nullable
    @UsedByGeneratedCode
    protected <V> V dispatch(int index, @NonNull B target, @Nullable Object[] args) {
        throw unknownDispatchAtIndexException(index);
    }

    /**
     * Triggers the invocation of the method at index.
     *
     * @param index  The method index
     * @param target The target
     * @param arg    The argument
     * @return The result
     */
    @Nullable
    @UsedByGeneratedCode
    protected Object dispatchOne(int index, @NonNull Object target, @Nullable Object arg) {
        throw unknownDispatchAtIndexException(index);
    }

    /**
     * Creates a new exception when the dispatch at index is not found.
     *
     * @param index The method index
     * @return The exception
     */
    @UsedByGeneratedCode
    protected final RuntimeException unknownDispatchAtIndexException(int index) {
        return new IllegalStateException("Unknown dispatch at index: " + index);
    }

    @Override
    public B instantiate() throws InstantiationException {
        throw new InstantiationException("No default constructor exists");
    }

    @NonNull
    @Override
    public B instantiate(boolean strictNullable, Object... arguments) throws InstantiationException {
        ArgumentUtils.requireNonNull("arguments", arguments);

        if (arguments.length == 0) {
            return instantiate();
        }

        final Argument<?>[] constructorArguments = getConstructorArguments();
        if (constructorArguments.length != arguments.length) {
            throw new InstantiationException("Argument count [" + arguments.length + "] doesn't match required argument count: " + constructorArguments.length);
        }

        for (int i = 0; i < constructorArguments.length; i++) {
            Argument<?> constructorArgument = constructorArguments[i];
            final Object specified = arguments[i];
            if (specified == null) {
                if (constructorArgument.isDeclaredNullable() || !strictNullable) {
                    continue;
                } else {
                    throw new InstantiationException("Null argument specified for [" + constructorArgument.getName() + "]. If this argument is allowed to be null annotate it with @Nullable");
                }
            }
            if (!ReflectionUtils.getWrapperType(constructorArgument.getType()).isInstance(specified)) {
                throw new InstantiationException("Invalid argument [" + specified + "] specified for argument: " + constructorArgument);
            }
        }

        return instantiateInternal(arguments);
    }

    @Override
    public BeanConstructor<B> getConstructor() {
        if (beanConstructor == null) {
            beanConstructor = new BeanConstructor<B>() {
                @Override
                public Class<B> getDeclaringBeanType() {
                    return beanType;
                }

                @Override
                public Argument<?>[] getArguments() {
                    return constructorArguments;
                }

                @Override
                public B instantiate(Object... parameterValues) {
                    return AbstractInitializableBeanIntrospection.this.instantiate(parameterValues);
                }

                @Override
                public AnnotationMetadata getAnnotationMetadata() {
                    return constructorAnnotationMetadata;
                }
            };
        }
        return beanConstructor;
    }

    @Override
    public Argument<?>[] getConstructorArguments() {
        return constructorArguments;
    }

    @NonNull
    @Override
    public Optional<BeanProperty<B, Object>> getIndexedProperty(@NonNull Class<? extends Annotation> annotationType, @NonNull String annotationValue) {
//        ArgumentUtils.requireNonNull("annotationType", annotationType);
//        if (indexedValues != null && StringUtils.isNotEmpty(annotationValue)) {
//            return Optional.ofNullable(
//                    indexedValues.get(new AnnotationValueKey(annotationType, annotationValue))
//            );
//        }
        return Optional.empty();
    }

    @NonNull
    @Override
    public Optional<BeanProperty<B, Object>> getProperty(@NonNull String name) {
        ArgumentUtils.requireNonNull("name", name);
        BeanProperty<B, Object> property = findProperty(name);
        return Optional.ofNullable(property);
    }

    @NonNull
    @Override
    public Collection<BeanProperty<B, Object>> getIndexedProperties(@NonNull Class<? extends Annotation> annotationType) {
//        ArgumentUtils.requireNonNull("annotationType", annotationType);
//        if (indexed != null) {
//            final List<BeanProperty<T, Object>> indexed = this.indexed.get(annotationType);
//            if (indexed != null) {
//                return Collections.unmodifiableCollection(indexed);
//            }
//        }
        return Collections.emptyList();
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return annotationMetadata;
    }

    @NonNull
    @Override
    public Collection<BeanProperty<B, Object>> getBeanProperties() {
        return Arrays.asList(beanProperties);
    }

    @NonNull
    @Override
    public Class<B> getBeanType() {
        return beanType;
    }

    @NonNull
    @Override
    public Collection<BeanMethod<B, Object>> getBeanMethods() {
        return Arrays.asList(beanMethods);
    }

//    /**
//     * Used to produce an index for particular annotation type. Method referenced by generated byte code and
//     * not for public consumption. Should be called after {@link #addProperty(BeanProperty)} if required.
//     *
//     * @param annotationType The annotation type
//     * @param propertyName   The property name
//     */
//    @SuppressWarnings("unused")
//    @Internal
//    @UsedByGeneratedCode
//    protected final void indexProperty(@NonNull Class<? extends Annotation> annotationType, @NonNull String propertyName) {
//        ArgumentUtils.requireNonNull("annotationType", annotationType);
//        if (StringUtils.isNotEmpty(propertyName)) {
//            final BeanProperty<B, Object> property = findProperty(propertyName);
//            if (property == null) {
//                throw new IllegalStateException("Invalid byte code generated during bean introspection. Call addProperty first!");
//            }
//            if (indexed == null) {
//                indexed = new HashMap<>(2);
//            }
//            final List<BeanProperty<T, Object>> indexed = this.indexed.computeIfAbsent(annotationType, aClass -> new ArrayList<>(2));
//
//            indexed.add(property);
//        }
//    }
//
//    /**
//     * Used to produce an index for particular annotation type. Method referenced by generated byte code and
//     * not for public consumption. Should be called after {@link #addProperty(BeanProperty)} if required.
//     *
//     * @param annotationType  The annotation type
//     * @param propertyName    The property name
//     * @param annotationValue The annotation value
//     */
//    @SuppressWarnings("unused")
//    @Internal
//    @UsedByGeneratedCode
//    protected final void indexProperty(
//            @NonNull Class<? extends Annotation> annotationType,
//            @NonNull String propertyName,
//            @NonNull String annotationValue) {
//        indexProperty(annotationType, propertyName);
//        if (StringUtils.isNotEmpty(annotationValue) && StringUtils.isNotEmpty(propertyName)) {
//            if (indexedValues == null) {
//                indexedValues = new HashMap<>(10);
//            }
//            final BeanProperty<T, Object> property = beanProperties.get(propertyName);
//            indexedValues.put(new AbstractBeanIntrospection.AnnotationValueKey(annotationType, annotationValue), property);
//        }
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractInitializableBeanIntrospection<?> that = (AbstractInitializableBeanIntrospection<?>) o;
        return Objects.equals(beanType, that.beanType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanType);
    }

    @Override
    public String toString() {
        return "BeanIntrospection{" +
                "type=" + beanType +
                '}';
    }

    private final class BeanPropertyImpl<P> implements BeanProperty<B, P> {

        private final BeanPropertyRef<P> ref;

        private BeanPropertyImpl(BeanPropertyRef<P> ref) {
            this.ref = ref;
        }

        @NonNull
        @Override
        public String getName() {
            return ref.argument.getName();
        }

        @NonNull
        @Override
        public Class<P> getType() {
            return ref.argument.getType();
        }

        @Override
        @NonNull
        public Argument<P> asArgument() {
            return ref.argument;
        }

        @NonNull
        @Override
        public BeanIntrospection<B> getDeclaringBean() {
            return AbstractInitializableBeanIntrospection.this;
        }

        @Override
        public AnnotationMetadata getAnnotationMetadata() {
            return ref.argument.getAnnotationMetadata();
        }

        @Nullable
        @Override
        public P get(@NonNull B bean) {
            ArgumentUtils.requireNonNull("bean", bean);
            if (!beanType.isInstance(bean)) {
                throw new IllegalArgumentException("Invalid bean [" + bean + "] for type: " + beanType);
            }
            if (isWriteOnly()) {
                throw new UnsupportedOperationException("Cannot read from a write-only property");
            }
            return (P) dispatchOne(ref.getMethodIndex, bean, null);
        }

        @Override
        public void set(@NonNull B bean, @Nullable P value) {
            ArgumentUtils.requireNonNull("bean", bean);

            if (!beanType.isInstance(bean)) {
                throw new IllegalArgumentException("Invalid bean [" + bean + "] for type: " + bean);
            }
            if (isReadOnly()) {
                throw new UnsupportedOperationException("Cannot write a read-only property: " + getName());
            }
            if (value != null && !ReflectionUtils.getWrapperType(getType()).isInstance(value)) {
                throw new IllegalArgumentException("Specified value [" + value + "] is not of the correct type: " + getType());
            }
            dispatchOne(ref.setMethodIndex, bean, value);
        }

        @Override
        public B withValue(@NonNull B bean, @Nullable P value) {
            ArgumentUtils.requireNonNull("bean", bean);
            if (!beanType.isInstance(bean)) {
                throw new IllegalArgumentException("Invalid bean [" + bean + "] for type: " + beanType);
            }
            if (value == get(bean)) {
                return bean;
            } else if (ref.withMethodIndex == -1) {
                return BeanProperty.super.withValue(bean, value);
            } else {
                return (B) dispatchOne(ref.withMethodIndex, bean, value);
            }
        }

        @Override
        public boolean isReadOnly() {
            return ref.readyOnly;
        }

        @Override
        public boolean isWriteOnly() {
            return ref.getMethodIndex == -1 && (ref.setMethodIndex != -1 || ref.withMethodIndex != -1);
        }

        @Override
        public boolean hasSetterOrConstructorArgument() {
            return ref.mutable;
        }

        @Override
        public String toString() {
            return "BeanProperty{" +
                    "beanType=" + beanType +
                    ", type=" + ref.argument.getType() +
                    ", name='" + ref.argument.getName() + '\'' +
                    '}';
        }
    }

    private final class BeanMethodImpl<P> implements BeanMethod<B, P> {

        private final BeanMethodRef<P> ref;

        private BeanMethodImpl(BeanMethodRef<P> ref) {
            this.ref = ref;
        }

        @NonNull
        @Override
        public BeanIntrospection<B> getDeclaringBean() {
            return AbstractInitializableBeanIntrospection.this;
        }

        @Override
        public @NonNull
        ReturnType<P> getReturnType() {
            //noinspection unchecked
            return new ReturnType() {
                @Override
                public Class<P> getType() {
                    return ref.returnType.getType();
                }

                @Override
                @NonNull
                public Argument<P> asArgument() {
                    return ref.returnType;
                }

                @Override
                public Map<String, Argument<?>> getTypeVariables() {
                    return ref.returnType.getTypeVariables();
                }

                @NonNull
                @Override
                public AnnotationMetadata getAnnotationMetadata() {
                    return ref.returnType.getAnnotationMetadata();
                }
            };
        }

        @NonNull
        @Override
        public AnnotationMetadata getAnnotationMetadata() {
            return ref.annotationMetadata == null ? AnnotationMetadata.EMPTY_METADATA : ref.annotationMetadata;
        }

        @NonNull
        @Override
        public String getName() {
            return ref.name;
        }

        @Override
        public Argument<?>[] getArguments() {
            return ref.arguments == null ? Argument.ZERO_ARGUMENTS : ref.arguments;
        }

        @Override
        public P invoke(@NonNull B instance, Object... arguments) {
            return dispatch(ref.methodIndex, instance, arguments);
        }

    }

    public static final class BeanPropertyRef<P> {
        @NonNull
        final Argument<P> argument;

        final int getMethodIndex;
        final int setMethodIndex;
        final int withMethodIndex;
        final boolean readyOnly;
        final boolean mutable;

        public BeanPropertyRef(@NonNull Argument<P> argument,
                               int getMethodIndex,
                               int setMethodIndex,
                               int valueMethodIndex,
                               boolean readyOnly, boolean mutable) {
            this.argument = argument;
            this.getMethodIndex = getMethodIndex;
            this.setMethodIndex = setMethodIndex;
            this.withMethodIndex = valueMethodIndex;
            this.readyOnly = readyOnly;
            this.mutable = mutable;
        }
    }

    public static final class BeanMethodRef<P> {
        @NonNull
        final Argument<P> returnType;
        @NonNull
        final String name;
        @Nullable
        final AnnotationMetadata annotationMetadata;
        @Nullable
        final Argument<?>[] arguments;

        final int methodIndex;

        public BeanMethodRef(@NonNull Argument<P> returnType,
                             @NonNull String name,
                             @Nullable AnnotationMetadata annotationMetadata,
                             @Nullable Argument<?>[] arguments,
                             int methodIndex) {
            this.returnType = returnType;
            this.name = name;
            this.annotationMetadata = annotationMetadata;
            this.arguments = arguments;
            this.methodIndex = methodIndex;
        }
    }

//    /**
//     * Used as a key to lookup indexed annotation values.
//     */
//    private static final class AnnotationValueKey {
//        final @NonNull
//        Class<? extends Annotation> type;
//        final @NonNull
//        String value;
//
//        AnnotationValueKey(@NonNull Class<? extends Annotation> type, @NonNull String value) {
//            this.type = type;
//            this.value = value;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) {
//                return true;
//            }
//            if (o == null || getClass() != o.getClass()) {
//                return false;
//            }
//            AbstractBeanIntrospection.AnnotationValueKey that = (AbstractBeanIntrospection.AnnotationValueKey) o;
//            return type.equals(that.type) &&
//                    value.equals(that.value);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(type, value);
//        }
//    }
}
