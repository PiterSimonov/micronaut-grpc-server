/*
 * Copyright 2018 Enegate
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

package com.enegate.micronaut.grpc.server

import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.context.event.StartupEvent
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

/**
 * @author Steve Schneider
 */

class GrpcServerFactorySpec extends Specification {

    void "test server builder factory"() {
        given:
        def applContext = new DefaultApplicationContext("test") {
            @Override
            void publishEvent(Object event) {
                if (!(event instanceof StartupEvent))
                    super.publishEvent(event)
            }
        }
        applContext.start()

        when:
        def serverBuilder = applContext.getBean(ServerBuilder)

        then:
        serverBuilder instanceof ServerBuilder
        !(serverBuilder instanceof InProcessServerBuilder)

        cleanup:
        applContext.close()
    }

    void "test inprocess server builder factory"() {
        given:
        def applContext = new DefaultApplicationContext("test") {
            @Override
            void publishEvent(Object event) {
                if (!(event instanceof StartupEvent))
                    super.publishEvent(event)
            }
        }
        applContext.environment.addPropertySource(PropertySource.of("test",
                ["micronaut.grpc.inprocess"    : "true",
                 "micronaut.grpc.inprocessname": "aName"]
        ))
        applContext.start()

        when:
        def serverBuilder = applContext.getBean(ServerBuilder)

        then:
        serverBuilder instanceof InProcessServerBuilder

        cleanup:
        applContext.close()
    }

    void "test inprocess server builder factory no inprocessname provided"() {
        given:
        def applContext = new DefaultApplicationContext("test") {
            @Override
            void publishEvent(Object event) {
                if (!(event instanceof StartupEvent))
                    super.publishEvent(event)
            }
        }
        applContext.environment.addPropertySource(PropertySource.of("test",
                ["micronaut.grpc.inprocess": "true"]
        ))
        applContext.start()

        when:
        def serverBuilder = applContext.getBean(ServerBuilder)

        then:
        thrown NoSuchBeanException
    }

}
