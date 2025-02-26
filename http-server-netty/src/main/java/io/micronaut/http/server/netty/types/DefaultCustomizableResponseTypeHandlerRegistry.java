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
package io.micronaut.http.server.netty.types;

import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link NettyCustomizableResponseTypeHandler} instances.
 *
 * @author James Kleeh
 * @since 1.0
 */
@Singleton
public class DefaultCustomizableResponseTypeHandlerRegistry implements NettyCustomizableResponseTypeHandlerRegistry {

    private List<NettyCustomizableResponseTypeHandler> handlers;
    private ConcurrentHashMap<Class<?>, Optional<NettyCustomizableResponseTypeHandler>> handlerCache = new ConcurrentHashMap<>(5);

    /**
     * @param typeHandlers The Netty customizable response type handlers
     */
    public DefaultCustomizableResponseTypeHandlerRegistry(NettyCustomizableResponseTypeHandler... typeHandlers) {
        this.handlers = Arrays.asList(typeHandlers);
    }


    /**
     * @param typeHandlers The Netty customizable response type handlers
     */
    @Inject public DefaultCustomizableResponseTypeHandlerRegistry(List<NettyCustomizableResponseTypeHandler> typeHandlers) {
        this.handlers = CollectionUtils.isNotEmpty(typeHandlers) ? typeHandlers : Collections.emptyList();
    }

    @Override
    public Optional<NettyCustomizableResponseTypeHandler> findTypeHandler(Class<?> type) {
        Optional<NettyCustomizableResponseTypeHandler> foundHandler = handlerCache.get(type);
        if (foundHandler != null) {
            return foundHandler;
        }
        Optional<NettyCustomizableResponseTypeHandler> optionalHandler = Optional.empty();
        for (NettyCustomizableResponseTypeHandler handler : handlers) {
            if (handler.supports(type)) {
                optionalHandler = Optional.of(handler);
                break;
            }
        }
        handlerCache.put(type, optionalHandler);
        return optionalHandler;
    }
}
