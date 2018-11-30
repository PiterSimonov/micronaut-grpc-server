package com.enegate.micronaut.grpc.server.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author Steve Schneider
 */

@ConfigurationProperties("micronaut.grpc")
public class GrpcServerConfiguration {
    private int port = 8081;
    private boolean reflection = false;
    private boolean healthcheck = false;
    private boolean inprocess = false;
    private String inprocessname = "";

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

    public boolean isHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(boolean healthcheck) {
        this.healthcheck = healthcheck;
    }

    public boolean isInprocess() {
        return inprocess;
    }

    public void setInprocess(boolean inprocess) {
        this.inprocess = inprocess;
    }

    public String getInprocessname() {
        return inprocessname;
    }

    public void setInprocessname(String inprocessname) {
        this.inprocessname = inprocessname;
    }
}
