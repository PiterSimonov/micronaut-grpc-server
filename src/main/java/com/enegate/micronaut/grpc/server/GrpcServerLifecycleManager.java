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
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.services.HealthStatusManager;
import io.micronaut.context.ApplicationContext;
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

/**
 * @author Steve Schneider
 */

@Singleton
public class GrpcServerLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerLifecycleManager.class);

    private GrpcServerConfiguration serverConfiguration;
    private ApplicationContext applicationContext;
    private ServerBuilder serverBuilder;
    private final AtomicBoolean running = new AtomicBoolean();
    private Server server;
    private HealthStatusManager healthStatusManager;

    @Inject
    public GrpcServerLifecycleManager(GrpcServerConfiguration serverConfiguration,
                                      ApplicationContext applicationContext,
                                      ServerBuilder serverBuilder
    ) {
        this.serverConfiguration = serverConfiguration;
        this.applicationContext = applicationContext;
        this.serverBuilder = serverBuilder;
    }

    @EventListener
    public synchronized void onStartup(StartupEvent event) throws IOException {
        if (running.get()) return;

        ArrayList<ServerInterceptor> globalInterceptors = findGlobalInterceptors();

        if (serverConfiguration.isHealthcheck()) {
            addHealthCheckService();
        }

        if (serverConfiguration.isReflection()) {
            addReflectionService();
        }

        addServices(globalInterceptors);

        callGrpcServerBuilderInterceptor();

        startServer();
    }

    private void callGrpcServerBuilderInterceptor() {
        applicationContext.getBeanDefinitions(GrpcServerBuilderInterceptor.class).stream()
                .filter(interceptorBeanDef -> interceptorBeanDef.hasAnnotation(GrpcInterceptor.class))
                .forEach(interceptorBeanDef -> {
                    GrpcServerBuilderInterceptor serverBuilderInterceptor = applicationContext.getBean(interceptorBeanDef.getBeanType());
                    serverBuilderInterceptor.intercept(serverBuilder);
                });
    }

    private void startServer() throws IOException {
        try {
            server = serverBuilder.build().start();

            Thread thread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server stopped unexpectedly");
                }
            });
            thread.start();

            running.set(true);
            LOG.info("gRPC server running on port: " + server.getPort());
            this.applicationContext.publishEvent(new GrpcServerStartupEvent(this));

        } catch (IOException e) {
            LOG.error("gRPC server cannot be started");
            throw e;
        }
    }

    private void addHealthCheckService() {
        healthStatusManager = new HealthStatusManager();
        serverBuilder.addService(healthStatusManager.getHealthService());
        LOG.debug("Adding gRPC service: " + HealthGrpc.getServiceDescriptor().getName());
    }

    private void addReflectionService() {
        serverBuilder.addService(ProtoReflectionService.newInstance());
        LOG.debug("Adding gRPC service: " + ServerReflectionGrpc.getServiceDescriptor().getName());
    }

    private void addServices(ArrayList<ServerInterceptor> globalInterceptors) {

        applicationContext.getBeanDefinitions(BindableService.class).stream()
                .filter(serviceBeanDef -> serviceBeanDef.hasAnnotation(GrpcService.class))
                .forEach(serviceBeanDef -> {
                    ArrayList<ServerInterceptor> interceptors = findServiceInterceptors(serviceBeanDef);

                    BindableService serviceBean = applicationContext.getBean(serviceBeanDef.getBeanType());

                    ArrayList<ServerInterceptor> allInterceptors = new ArrayList<>();
                    allInterceptors.addAll(globalInterceptors);
                    allInterceptors.addAll(interceptors);
                    ServerServiceDefinition serviceDef = ServerInterceptors.intercept(serviceBean, allInterceptors);

                    //Add service
                    serverBuilder.addService(serviceDef);

                    // Set health check status for service
                    if (serverConfiguration.isHealthcheck()) {
                        healthStatusManager.setStatus(serviceDef.getServiceDescriptor().getName(), HealthCheckResponse.ServingStatus.SERVING);
                    }

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
    }

    @SuppressWarnings("unchecked")
    private ArrayList<ServerInterceptor> findServiceInterceptors(BeanDefinition<BindableService> serviceBeanDef) {
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
                    interceptors.add(interceptorBean);
                }
            }
        }
        return interceptors;
    }

    private ArrayList<ServerInterceptor> findGlobalInterceptors() {
        ArrayList<ServerInterceptor> globalInterceptors = new ArrayList<>();
        applicationContext.getBeanDefinitions(ServerInterceptor.class).stream()
                .filter(interceptorBeanDef -> interceptorBeanDef.hasAnnotation(GrpcInterceptor.class))
                .forEach(interceptorBeanDef -> {
                    AnnotationValue<GrpcInterceptor> grpcInterceptorAnno = interceptorBeanDef.getAnnotation(GrpcInterceptor.class);
                    if (grpcInterceptorAnno != null) {
                        Optional<Boolean> isGlobal = grpcInterceptorAnno.get("global", Boolean.class);
                        if (isGlobal.isPresent() && isGlobal.get()) {
                            ServerInterceptor interceptorBean = applicationContext.getBean(interceptorBeanDef.getBeanType());
                            globalInterceptors.add(interceptorBean);
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
        return globalInterceptors;
    }

    @EventListener
    public synchronized void onShutdown(ShutdownEvent event) {
        if (running.compareAndSet(true, false)) {
            // Clear health check status for all registered services
            server.getServices().forEach(service -> healthStatusManager.clearStatus(service.getServiceDescriptor().getName()));
            try {
                server.shutdown();
                LOG.info("gRPC server stopped");
                applicationContext.publishEvent(new GrpcServerShutdownEvent(this));
            } catch (Throwable e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error stopping gRPC server: " + e.getMessage(), e);
                }
            }
        }
    }

}
