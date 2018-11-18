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
