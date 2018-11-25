package com.enegate.micronaut.grpc.server.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("micronaut.grpc")
public class GrpcServerConfiguration {
    private int port = 8081;
    private boolean reflection = false;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isReflection() {
        return reflection;
    }

    public void setReflection(boolean reflection) {
        this.reflection = reflection;
    }
}
