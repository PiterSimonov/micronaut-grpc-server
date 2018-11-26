package com.enegate.micronaut.grpc.server;

import com.enegate.micronaut.grpc.server.configuration.GrpcServerConfiguration;
import io.grpc.ServerBuilder;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class GrpcServerFactory {

    @Singleton
    ServerBuilder serverBuilder(GrpcServerConfiguration serverConfiguration) {
        return ServerBuilder.forPort(serverConfiguration.getPort());
    }

}
