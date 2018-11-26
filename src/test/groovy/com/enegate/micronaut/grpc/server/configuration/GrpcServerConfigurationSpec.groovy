package com.enegate.micronaut.grpc.server.configuration


import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.context.event.StartupEvent
import spock.lang.Specification

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
