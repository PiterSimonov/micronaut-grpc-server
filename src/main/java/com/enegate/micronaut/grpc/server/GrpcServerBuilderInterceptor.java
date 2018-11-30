package com.enegate.micronaut.grpc.server;

import io.grpc.ServerBuilder;

/**
 * @author Steve Schneider
 */

public interface GrpcServerBuilderInterceptor {
    void intercept(ServerBuilder serverBuilder);
}
