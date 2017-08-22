/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.particleframework.inject.factory.factorydefinition

import org.particleframework.context.BeanContext
import org.particleframework.context.DefaultBeanContext
import spock.lang.Ignore
import spock.lang.Specification

class FactorySpec extends Specification {

    @Ignore
    void "test factory definition"() {
        given:
        BeanContext beanContext = new DefaultBeanContext().start()

        expect:
        beanContext.getBean(BFactory)
        beanContext.getBean(B) != null
        beanContext.getBean(B) == beanContext.getBean(B)
        beanContext.getBean(C) != beanContext.getBean(C)
        beanContext.getBean(C).b == beanContext.getBean(B)
        beanContext.getBean(B).name == "FROMFACTORY"

    }
}
