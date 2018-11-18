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

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.Internal
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.runtime.ApplicationConfiguration
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.event.ApplicationShutdownEvent
import io.micronaut.runtime.event.ApplicationStartupEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton
import kotlin.concurrent.thread

@Bean
annotation class GrpcService

@Singleton
@Internal
class GrpcServer(val applContext: ApplicationContext, val applConfiguration: ApplicationConfiguration) : EmbeddedApplication<GrpcServer> {

    private val serverPort = 9090

    private val LOG = LoggerFactory.getLogger(GrpcServer::class.java)

    private val running: AtomicBoolean = AtomicBoolean()
    private lateinit var server: Server

    override fun getApplicationContext(): ApplicationContext = applContext
    override fun getApplicationConfiguration(): ApplicationConfiguration = applConfiguration
    override fun isServer(): Boolean = true
    override fun isRunning(): Boolean = running.get() && !SocketUtils.isTcpPortAvailable(serverPort)

    @Synchronized
    override fun start(): GrpcServer {
        if (isRunning) return this

        val builder = ServerBuilder.forPort(serverPort)

        for (beanDef in applContext.allBeanDefinitions) {
            if (!beanDef.hasAnnotation(GrpcService::class.java))
                continue
            if (!BindableService::class.java.isAssignableFrom(beanDef.beanType))
                continue
            val bean = applContext.getBean(beanDef.beanType) as BindableService
            val serviceDef = bean.bindService()
            builder.addService(serviceDef)
            LOG.info("Adding gRPC service: ${serviceDef.serviceDescriptor.name}")
            if (LOG.isDebugEnabled)
                LOG.debug("gRPC service [${serviceDef.serviceDescriptor.name}] is implemented in class [${beanDef.name}]")
        }

        server = builder.build().start()

        thread { server.awaitTermination() }

        running.set(true)
        LOG.info("gRPC server running on port ${serverPort}")
        applContext.publishEvent(ApplicationStartupEvent(this))
        return this
    }

    @Synchronized
    override fun stop(): GrpcServer {
        if (isRunning && running.compareAndSet(true, false)) {
            try {
                server.shutdown()
                LOG.info("gRPC server stopped")
                if (applContext.isRunning) {
                    applContext.stop()
                    applContext.publishEvent(ApplicationShutdownEvent(this))
                }
            } catch (e: Throwable) {
                if (LOG.isErrorEnabled) {
                    LOG.error("Error stopping Micronaut gRPC server: " + e.message, e)
                }
            }
        }
        return this
    }
}
