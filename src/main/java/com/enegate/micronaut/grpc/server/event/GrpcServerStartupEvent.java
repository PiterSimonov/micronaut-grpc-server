package com.enegate.micronaut.grpc.server.event;

import com.enegate.micronaut.grpc.server.GrpcServerLifecycleManager;
import io.micronaut.context.event.ApplicationEvent;

/**
 * @author Steve Schneider
 */

public class GrpcServerStartupEvent extends ApplicationEvent {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public GrpcServerStartupEvent(GrpcServerLifecycleManager source) {
        super(source);
    }
}
