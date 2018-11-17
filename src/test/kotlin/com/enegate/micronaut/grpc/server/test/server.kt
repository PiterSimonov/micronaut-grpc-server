package com.enegate.micronaut.grpc.server.test

import com.enegate.micronaut.grpc.server.GrpcServer
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.EmbeddedApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object GrpcServerFeature : Spek({
    Feature("GrpcServer") {
        lateinit var grpcServer: EmbeddedApplication<*>

        Scenario("start grpc server") {
            When("starting a grpc server") {
                grpcServer = ApplicationContext.run(EmbeddedApplication::class.java)
            }

            Then("it should run") {
                assertEquals(true, grpcServer.isRunning)
            }
        }

        Scenario("stop grpc server") {
            When("stopping a grpc server") {
                grpcServer.applicationContext.stop()
            }

            Then("it should not run") {
                assertEquals(false, grpcServer.isRunning)
            }
        }
    }
})
