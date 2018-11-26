package com.enegate.micronaut.grpc.server

import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.context.event.StartupEvent
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

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
