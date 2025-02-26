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
package io.micronaut.context.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>Annotation applied to the method or type indicating that a {@link io.micronaut.inject.ExecutableMethod} should be
 * produced for this method.</p>
 * <p>
 * <p>When applied to a type all public methods will be considered executable and the necessary metadata produced</p>
 * <p>
 * <p>This annotation can be used as a meta annotation</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Executable {

    /**
     * Whether the {@link io.micronaut.inject.ExecutableMethod} should be processed at startup by the registered
     * {@link io.micronaut.context.processor.ExecutableMethodProcessor} instances. The default is false to ensure fast
     * startup, but for certain types of components processing at startup is required (for example scheduled tasks)
     *
     * @return Whether to process the {@link io.micronaut.inject.ExecutableMethod} at startup
     */
    boolean processOnStartup() default false;
}
