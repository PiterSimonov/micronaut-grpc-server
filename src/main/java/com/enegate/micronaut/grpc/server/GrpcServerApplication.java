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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextLifeCycle;
import io.micronaut.context.annotation.Requires;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.runtime.event.ApplicationShutdownEvent;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import io.micronaut.runtime.exceptions.ApplicationStartupException;

import javax.inject.Singleton;

/**
 * This code is based on io.micronaut.messaging.MessagingApplication

 * An alternative {@link EmbeddedApplication} that gets activated for grpc server applications when
 * no other application is present.
 *
 * @author Steve Schneider
 */

@Singleton
@Requires(missingBeans = EmbeddedApplication.class)
public class GrpcServerApplication implements EmbeddedApplication {

    private final ApplicationContext applicationContext;
    private final ApplicationConfiguration configuration;

    public GrpcServerApplication(ApplicationContext applicationContext, ApplicationConfiguration configuration) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public ApplicationConfiguration getApplicationConfiguration() {
        return configuration;
    }

    @Override
    public boolean isRunning() {
        return applicationContext.isRunning();
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public GrpcServerApplication start() {
        ApplicationContext applicationContext = getApplicationContext();
        if (applicationContext != null && !applicationContext.isRunning()) {
            try {
                applicationContext.start();
                applicationContext.publishEvent(new ApplicationStartupEvent(this));
            } catch (Throwable e) {
                throw new ApplicationStartupException("Error starting grpc server application: " + e.getMessage(), e);
            }
        }
        return this;
    }

    @Override
    public ApplicationContextLifeCycle stop() {
        ApplicationContext applicationContext = getApplicationContext();
        if (applicationContext != null && applicationContext.isRunning()) {
            applicationContext.stop();
            applicationContext.publishEvent(new ApplicationShutdownEvent(this));
        }
        return this;
    }

}
