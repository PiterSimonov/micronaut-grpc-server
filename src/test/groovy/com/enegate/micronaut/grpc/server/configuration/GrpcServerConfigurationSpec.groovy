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

package com.enegate.micronaut.grpc.server.configuration

import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.context.event.StartupEvent
import spock.lang.Specification

/**
 * @author Steve Schneider
 */

class GrpcServerConfigurationSpec extends Specification {

    void "test defaults"() {
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
        def config = applContext.getBean(GrpcServerConfiguration)

        then:
        config.port == 8081
        config.reflection == false
        config.healthcheck == false
        config.inprocess == false
        config.inprocessname == ""

        cleanup:
        applContext.close()
    }

    void "test set properties"() {
        given:
        def applContext = new DefaultApplicationContext("test") {
            @Override
            void publishEvent(Object event) {
                if (!(event instanceof StartupEvent))
                    super.publishEvent(event)
            }
        }
        applContext.environment.addPropertySource(PropertySource.of("test",
                ["micronaut.grpc.port"         : "1234",
                 "micronaut.grpc.reflection"   : "true",
                 "micronaut.grpc.healthcheck"  : "true",
                 "micronaut.grpc.inprocess"    : "true",
                 "micronaut.grpc.inprocessname": "aName"]
        ))
        applContext.start()

        when:
        def config = applContext.getBean(GrpcServerConfiguration)

        then:
        config.port == 1234
        config.reflection == true
        config.healthcheck == true
        config.inprocess == true
        config.inprocessname == "aName"

        cleanup:
        applContext.close()
    }

}
