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

package com.enegate.micronaut.grpc.server;

import com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor;
import com.enegate.micronaut.grpc.server.annotation.GrpcService;
import com.enegate.micronaut.grpc.server.configuration.GrpcServerConfiguration;
import com.enegate.micronaut.grpc.server.event.GrpcServerShutdownEvent;
import com.enegate.micronaut.grpc.server.event.GrpcServerStartupEvent;
import io.grpc.*;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class GrpcServerLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerLifecycleManager.class);

    //TODO: Read from configuration file
    private GrpcServerConfiguration serverConfiguration;
    private ApplicationContext applicationContext;
    private final AtomicBoolean running = new AtomicBoolean();
    private Server server;

    @Inject
    public GrpcServerLifecycleManager(GrpcServerConfiguration serverConfiguration, ApplicationContext applicationContext) {
        this.serverConfiguration = serverConfiguration;
        this.applicationContext = applicationContext;
    }

    @EventListener
    @SuppressWarnings("unchecked")
    public synchronized void onStartup(StartupEvent event) throws IOException {
        if (running.get()) return;

        ServerBuilder builder = ServerBuilder.forPort(serverConfiguration.getPort());

        //Find global interceptors
        ArrayList<ServerInterceptor> globalInterceptors = new ArrayList<>();
        applicationContext.getBeanDefinitions(ServerInterceptor.class).stream()
                .filter(interceptorBeanDef -> interceptorBeanDef.hasAnnotation(GrpcInterceptor.class))
                .forEach(interceptorBeanDef -> {
                    AnnotationValue<GrpcInterceptor> grpcInterceptorAnno = interceptorBeanDef.getAnnotation(GrpcInterceptor.class);
                    if (grpcInterceptorAnno != null) {
                        Optional<Boolean> isGlobal = grpcInterceptorAnno.get("global", Boolean.class);
                        if (isGlobal.isPresent() && isGlobal.get()) {
                            ServerInterceptor interceptorBean = applicationContext.getBean(interceptorBeanDef.getBeanType());
                            if (interceptorBean != null) {
                                globalInterceptors.add(interceptorBean);
                            }
                        }
                    }
                });
        if (LOG.isDebugEnabled()) {
            globalInterceptors.forEach(si -> {
                LOG.debug("Adding global gRPC interceptor: " + si.getClass().getSimpleName());
                if (LOG.isTraceEnabled())
                    LOG.trace("Global gRPC interceptor [" + si.getClass().getSimpleName() + "] is implemented in class [" + si.getClass().getName() + "]");
            });
        }
        //Find services
        applicationContext.getBeanDefinitions(BindableService.class).stream()
                .filter(serviceBeanDef -> serviceBeanDef.hasAnnotation(GrpcService.class))
                .forEach(serviceBeanDef -> {
                    //Find service interceptors
                    ArrayList<ServerInterceptor> interceptors = new ArrayList<>();
                    AnnotationValue<GrpcService> grpcServiceAnno = serviceBeanDef.getAnnotation(GrpcService.class);
                    if (grpcServiceAnno != null) {
                        Optional<Class[]> interceptorsClasses = grpcServiceAnno.get("interceptors", Class[].class);
                        if (interceptorsClasses.isPresent()) {
                            for (Class<ServerInterceptor> aClass : interceptorsClasses.get()) {
                                BeanDefinition<ServerInterceptor> interceptorBeanDef = applicationContext.getBeanDefinition(aClass);
                                AnnotationValue<GrpcInterceptor> grpcInterceptorAnno = interceptorBeanDef.getAnnotation(GrpcInterceptor.class);
                                if (grpcInterceptorAnno != null) {
                                    Optional<Boolean> isGlobal = grpcInterceptorAnno.get("global", Boolean.class);
                                    if (isGlobal.isPresent() && isGlobal.get())
                                        continue;
                                }

                                ServerInterceptor interceptorBean = applicationContext.getBean(interceptorBeanDef.getBeanType());
                                if (interceptorBean != null) {
                                    interceptors.add(interceptorBean);
                                }
                            }
                        }
                    }

                    BindableService serviceBean = applicationContext.getBean(serviceBeanDef.getBeanType());

                    ArrayList<ServerInterceptor> allInterceptors = new ArrayList<>();
                    allInterceptors.addAll(globalInterceptors);
                    allInterceptors.addAll(interceptors);
                    ServerServiceDefinition serviceDef = ServerInterceptors.intercept(serviceBean, allInterceptors);

                    //Add service
                    builder.addService(serviceDef);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding gRPC service: " + serviceDef.getServiceDescriptor().getName());
                        interceptors.forEach(si -> {
                            LOG.debug("Adding gRPC interceptor for service " + serviceDef.getServiceDescriptor().getName() + ": " + si.getClass().getSimpleName());
                            if (LOG.isTraceEnabled())
                                LOG.trace("gRPC interceptor [" + si.getClass().getSimpleName() + "] is implemented in class [" + si.getClass().getName() + "]");
                        });
                    }

                    if (LOG.isTraceEnabled())
                        LOG.trace("gRPC service [" + serviceDef.getServiceDescriptor().getName() + "] is implemented in class [" + serviceBeanDef.getName() + "]");
                });

        if (serverConfiguration.isReflection()) {
            builder.addService(ProtoReflectionService.newInstance());
            LOG.debug("Adding gRPC service: " + ServerReflectionGrpc.getServiceDescriptor().getName());
        }

        try {
            server = builder.build().start();

            Thread thread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server stopped unexpectedly");
                }
            });
            thread.start();

            running.set(true);
            this.applicationContext.publishEvent(new GrpcServerStartupEvent(this));
            LOG.info("gRPC server running on port: " + serverConfiguration.getPort());

        } catch (IOException e) {
            LOG.error("gRPC server cannot be started");
            throw e;
        }
    }

    @EventListener
    public synchronized void onShutdown(ShutdownEvent event) {
        if (running.compareAndSet(true, false)) {
            try {
                server.shutdown();
                applicationContext.publishEvent(new GrpcServerShutdownEvent(this));
                LOG.info("gRPC server stopped");
            } catch (Throwable e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error stopping gRPC server: " + e.getMessage(), e);
                }
            }
        }
    }

}