package com.enegate.micronaut.grpc.server;

import com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor;
import com.enegate.micronaut.grpc.server.annotation.GrpcService;
import io.grpc.*;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class GrpcServerMethodAdapterAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerMethodAdapterAdvice.class);

    //TODO: Read from configuration file
    private int serverPort = 9090;

    private final AtomicBoolean running = new AtomicBoolean();
    private Server server = null;

    @EventListener
    public void onStartup(StartupEvent event) {
        if (running.get()) return;

        BeanContext beanContext = event.getSource();

        ServerBuilder builder = ServerBuilder.forPort(serverPort);

        //Find global interceptors
        ArrayList globalInterceptors = new ArrayList<ServerInterceptor>();
        beanContext.getBeanDefinitions(ServerInterceptor.class).stream()
                .filter(interceptorBeanDef -> interceptorBeanDef.hasAnnotation(GrpcInterceptor.class))
                .forEach(interceptorBeanDef -> {
                    AnnotationValue<GrpcInterceptor> grpcInterceptorAnno = interceptorBeanDef.getAnnotation(GrpcInterceptor.class);
                    if (grpcInterceptorAnno != null) {
                        Optional<Boolean> isGlobal = grpcInterceptorAnno.get("global", Boolean.class);
                        if (isGlobal.isPresent() && isGlobal.get()) {
                            ServerInterceptor interceptorBean = beanContext.getBean(interceptorBeanDef.getBeanType());
                            globalInterceptors.add(interceptorBean);
                        }
                    }
                });
        globalInterceptors.forEach(si -> {
            LOG.info("Adding global gRPC interceptor: " + si.getClass().getSimpleName());
            if (LOG.isDebugEnabled())
                LOG.debug("Global gRPC interceptor [" + si.getClass().getSimpleName() + "] is implemented in class [" + si.getClass().getName() + "]");
        });

        //Find services
        beanContext.getBeanDefinitions(BindableService.class).stream()
                .filter(serviceBeanDef -> serviceBeanDef.hasAnnotation(GrpcService.class))
                .forEach(serviceBeanDef -> {
                    //Find service interceptors
                    ArrayList interceptors = new ArrayList<ServerInterceptor>();
                    AnnotationValue<GrpcService> grpcServiceAnno = serviceBeanDef.getAnnotation(GrpcService.class);
                    if (grpcServiceAnno != null) {
                        Optional<Class[]> interceptorsClasses = grpcServiceAnno.get("interceptors", Class[].class);
                        if (interceptorsClasses.isPresent()) {
                            for (Class aClass : interceptorsClasses.get()) {
                                ServerInterceptor interceptorBean = (ServerInterceptor) beanContext.getBean(aClass);
                                interceptors.add(interceptorBean);
                            }
                        }
                    }

                    BindableService serviceBean = beanContext.getBean(serviceBeanDef.getBeanType());
                    interceptors.addAll(globalInterceptors);
                    ServerServiceDefinition serviceDef = ServerInterceptors.intercept(serviceBean, interceptors);

                    //Add service
                    builder.addService(serviceDef);
                    LOG.info("Adding gRPC service: " + serviceDef.getServiceDescriptor().getName());
                    interceptors.forEach(si -> {
                        LOG.info("Adding gRPC interceptor for service " + serviceDef.getServiceDescriptor().getName() + ": " + si.getClass().getSimpleName());
                        if (LOG.isDebugEnabled())
                            LOG.debug("gRPC interceptor [" + si.getClass().getSimpleName() + "] is implemented in class [" + si.getClass().getName() + "]");
                    });

                    if (LOG.isDebugEnabled())
                        LOG.debug("gRPC service [" + serviceDef.getServiceDescriptor().getName() + "] is implemented in class [" + serviceBeanDef.getName() + "]");
                });

        try {
            server = builder.build().start();

            Thread thread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server stopped unexpectedly: ", e);
                }
            });
            thread.start();

            running.set(true);
            LOG.info("gRPC server running on port: " + serverPort);

        } catch (IOException e) {
            LOG.error("gRPC server cannot be started: ", e);
        }
    }

    @EventListener
    public void onShutdown(ShutdownEvent event) {
        if (running.compareAndSet(true, false)) {
            try {
                server.shutdown();
                LOG.info("gRPC server stopped");
            } catch (Throwable e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error stopping gRPC server: " + e.getMessage(), e);
                }
            }
        }
    }

}
