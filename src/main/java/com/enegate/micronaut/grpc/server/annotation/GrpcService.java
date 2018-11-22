package com.enegate.micronaut.grpc.server.annotation;

import io.grpc.ServerInterceptor;
import io.micronaut.context.annotation.Bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Bean
public @interface GrpcService {
    Class<? extends ServerInterceptor>[] interceptors() default {};
}
