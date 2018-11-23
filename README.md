# [gRPC](http://grpc.io) server support for [Micronaut](http://micronaut.io/)
This project is inspired by the [Spring boot starter for gRPC](https://github.com/LogNet/grpc-spring-boot-starter) module and backed by the [Java gRPC implementation](https://github.com/grpc/grpc-java).

## Features
- Configures and runs the embedded gRPC server
- Global and per service interceptors

## Usage

### Dependencies
Add a dependency using Maven:

````xml
<dependency>
  <groupId>TBD</groupId>
  <artifactId>TBD</artifactId>
  <version>TBD</version>
</dependency>
````

Add a dependency using Gradle:

````gradle
dependencies {
  compile 'TBD'
}
````

### gRPC server
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

### Interceptor

#### Per Service
Annotate your interceptor implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor``

````java
@GrpcInterceptor
public class GreeterServiceInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(call, headers);
    }
}
````

Add the interceptor to the ``@GrpcService`` annotation of your server interface implementation

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
Annotate your interceptor implementation with ``@com.enegate.micronaut.grpc.server.annotation.GrpcInterceptor`` and set the ``global`` parameter to ``true``

````java
@GrpcInterceptor(global = true)
public class GlobalInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(call, headers);
    }
}
````

## Examples

- [micronaut-grpc-example](https://github.com/Enegate/micronaut-grpc-example) (Java)
- [micronaut-grpc-example-kotlin]() (Kotlin)
