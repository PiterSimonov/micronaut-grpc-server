# [gRPC](http://grpc.io) server support for [Micronaut](http://micronaut.io/) (HTTP/2)

[![Build Status](https://travis-ci.org/Enegate/micronaut-grpc-server.svg?branch=master)](https://travis-ci.org/Enegate/micronaut-grpc-server)

This project is inspired by the [Spring boot starter for gRPC](https://github.com/LogNet/grpc-spring-boot-starter) module and backed by the [Java gRPC implementation](https://github.com/grpc/grpc-java).

## Features
- Configures and runs the embedded gRPC server
  - Reason: Micronaut does not support HTTP/2 at the moment
- Global and per service interceptors
- Custom ServerBuilder configuration
- Fast startup time

## Usage

### Dependencies
Artifacts are published to Maven Central.

#### Maven
````xml
<dependency>
  <groupId>com.enegate</groupId>
  <artifactId>micronaut-grpc-server</artifactId>
  <version>0.0.1</version>
</dependency>
````

#### Gradle
````gradle
dependencies {
  compile 'com.enegate:micronaut-grpc-server:0.0.1'
}
````

### gRPC server

#### Implementation
Prerequisite: [Generate](https://github.com/google/protobuf-gradle-plugin) artifacts from your Protocol Buffer ``.proto`` definition files

Extract from an example ``.proto`` file:

````proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
````

Annotate your server interface implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcService``

````java
@GrpcService
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
````

#### Configuration

##### Port
The default port for the grpc server is ``8081``.

If you want to change the port add the ``port`` property to your ``application.yml`` file

````yaml
micronaut:
  grpc:
    port: 9876
````

##### Reflection
To enable the [reflection service](https://github.com/grpc/grpc-java/blob/master/services/src/main/proto/io/grpc/reflection/v1alpha/reflection.proto) add the ``reflection`` property to your ``application.yml`` file

````yaml
micronaut:
  grpc:
    reflection: true
````

##### Health check
To enable the [health check service](https://github.com/grpc/grpc-java/blob/master/services/src/main/proto/grpc/health/v1/health.proto) add the ``healthcheck`` property to your ``application.yml`` file

````yaml
micronaut:
  grpc:
    healthcheck: true
````

### Interceptor

#### Per Service
Annotate your interceptor implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor``
and implement Interface ``io.grpc.ServerInterceptor``

````java
@GrpcInterceptor
public class GreeterServiceInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(call, headers);
    }
}
````

Add the interceptor to the array parameter ``interceptors`` of the ``@GrpcService`` annotation  of your server interface implementation

````java
@GrpcService(interceptors = {GreeterServiceInterceptor.class})
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
````

#### Global
Annotate your interceptor implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor`` and set the ``global`` parameter to ``true``.

Implement Interface ``io.grpc.ServerInterceptor``

````java
@GrpcInterceptor(global = true)
public class GlobalInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(call, headers);
    }
}
````

#### ServerBuilder
To configure TLS etc.

Annotate your interceptor implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor``
and implement Interface ``com.enegate.micronaut.grpc.server.GrpcServerBuilderInterceptor``

````java
@GrpcInterceptor
public class ExampleServerBuilderInterceptor implements GrpcServerBuilderInterceptor {
    @Override
    public void intercept(ServerBuilder serverBuilder) {
        // Configure TLS
        serverBuilder.useTransportSecurity(...);
    }
}

````

## Examples

- [micronaut-grpc-example](https://github.com/Enegate/micronaut-grpc-example) (Java)
- [micronaut-grpc-example-kotlin](https://github.com/Enegate/micronaut-grpc-example-kotlin) (Kotlin)
