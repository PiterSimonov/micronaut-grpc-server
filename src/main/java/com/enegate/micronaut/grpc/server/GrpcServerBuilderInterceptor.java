package com.enegate.micronaut.grpc.server;

import io.grpc.ServerBuilder;

public interface GrpcServerBuilderInterceptor {
    void intercept(ServerBuilder serverBuilder);
}
