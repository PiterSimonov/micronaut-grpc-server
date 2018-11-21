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

import io.grpc.*
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
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention
@Bean
annotation class GrpcService(val interceptors: Array<KClass<out ServerInterceptor>> = emptyArray())

@Target(AnnotationTarget.CLASS)
@Retention
@Bean
annotation class GrpcInterceptor(val global: Boolean = false)

@Singleton
@Internal
class GrpcServer(val applContext: ApplicationContext, val applConfiguration: ApplicationConfiguration) : EmbeddedApplication<GrpcServer> {

    //TODO: Read from configuration file
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

        //Find global interceptors
        val globalInterceptors = mutableListOf<ServerInterceptor>()
        applContext.getBeanDefinitions(ServerInterceptor::class.java)
                .filter { it.hasAnnotation(GrpcInterceptor::class.java) }
                .forEach { interceptorBeanDef ->
                    val grpcInterceptorAnno = interceptorBeanDef.getAnnotation<GrpcInterceptor>(GrpcInterceptor::class.java)
                    if (grpcInterceptorAnno != null) {
                        val isGlobal = grpcInterceptorAnno.get("global", Boolean::class.java)
                        if (isGlobal.isPresent && isGlobal.get()) {
                            val interceptorBean = applContext.getBean(interceptorBeanDef.beanType) as ServerInterceptor
                            globalInterceptors.add(interceptorBean)
                        }
                    }
                }
        globalInterceptors.forEach {
            LOG.info("Adding global gRPC interceptor: ${it.javaClass.simpleName}")
            if (LOG.isDebugEnabled)
                LOG.debug("Global gRPC interceptor [${it.javaClass.simpleName}] is implemented in class [${it.javaClass.name}]")
        }

        //Find services
        applContext.getBeanDefinitions(BindableService::class.java)
                .filter { it.hasAnnotation(GrpcService::class.java) }
                .forEach { serviceBeanDef ->
                    //Find service interceptors
                    val interceptors = mutableListOf<ServerInterceptor>()
                    //TODO: Find a solution without synthesize()
                    // https://docs.micronaut.io/latest/guide/index.html#annotationMetadata
                    val grpcServiceAnnotation = serviceBeanDef.synthesize<GrpcService>(GrpcService::class.java)
                    for (interceptor in grpcServiceAnnotation.interceptors) {
                        val type = interceptor.javaObjectType
                        if (!ServerInterceptor::class.java.isAssignableFrom(type))
                            continue
                        val interceptorBeanDef = applContext.getBeanDefinition(type)
                        val grpcInterceptorAnno = interceptorBeanDef.getAnnotation<GrpcInterceptor>(GrpcInterceptor::class.java)
                        if (grpcInterceptorAnno != null) {
                            val isGlobal = grpcInterceptorAnno.get("global", Boolean::class.java)
                            if (isGlobal.isPresent && isGlobal.get())
                                continue
                        }
                        val interceptorBean = applContext.getBean(interceptorBeanDef.beanType) as ServerInterceptor
                        interceptors.add(interceptorBean)
                    }

                    val serviceBean = applContext.getBean(serviceBeanDef.beanType) as BindableService
                    val serviceDef = ServerInterceptors.intercept(serviceBean, globalInterceptors + interceptors)

                    //Add service
                    builder.addService(serviceDef)
                    LOG.info("Adding gRPC service: ${serviceDef.serviceDescriptor.name}")
                    interceptors.forEach {
                        LOG.info("Adding gRPC interceptor for ${serviceDef.serviceDescriptor.name}: ${it.javaClass.simpleName}")
                        if (LOG.isDebugEnabled)
                            LOG.debug("gRPC interceptor [${it.javaClass.simpleName}] is implemented in class [${it.javaClass.name}]")
                    }

                    if (LOG.isDebugEnabled)
                        LOG.debug("gRPC service [${serviceDef.serviceDescriptor.name}] is implemented in class [${serviceBeanDef.name}]")
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
                    LOG.error("Error stopping gRPC server: " + e.message, e)
                }
            }
        }
        return this
    }
}
