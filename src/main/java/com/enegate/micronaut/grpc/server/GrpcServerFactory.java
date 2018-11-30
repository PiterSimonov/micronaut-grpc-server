package com.enegate.micronaut.grpc.server;

import com.enegate.micronaut.grpc.server.configuration.GrpcServerConfiguration;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

/**
 * @author Steve Schneider
 */

@Factory
public class GrpcServerFactory {

    @Singleton
    @Requires(missingProperty = "micronaut.grpc.inprocess")
    ServerBuilder serverBuilder(GrpcServerConfiguration serverConfiguration) {
        return ServerBuilder.forPort(serverConfiguration.getPort());
    }

    @Singleton
    @Requires(property = "micronaut.grpc.inprocess")
    @Requires(property = "micronaut.grpc.inprocessname")
    ServerBuilder inProcessServerBuilder(GrpcServerConfiguration serverConfiguration) {
        return InProcessServerBuilder.forName(serverConfiguration.getInprocessname());
    }

}
