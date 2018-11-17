package com.enegate.micronaut.grpc.server

import io.grpc.Server
import io.grpc.ServerBuilder
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Internal
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.runtime.ApplicationConfiguration
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.event.ApplicationShutdownEvent
import io.micronaut.runtime.event.ApplicationStartupEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
@Internal
class GrpcServer(val applContext: ApplicationContext, val applConfiguration: ApplicationConfiguration) : EmbeddedApplication<GrpcServer> {

    private val serverPort = 9090

    private val LOG = LoggerFactory.getLogger(GrpcServer::class.java)

    private val running: AtomicBoolean = AtomicBoolean()
    private lateinit var server: Server

    override fun getApplicationContext(): ApplicationContext = applContext
    override fun getApplicationConfiguration(): ApplicationConfiguration = applConfiguration
    override fun isServer(): Boolean = true
    override fun isRunning(): Boolean = running.get() && !SocketUtils.isTcpPortAvailable(serverPort)

    @Synchronized
    override fun start(): GrpcServer {
        if (isRunning) return this

        val builder = ServerBuilder.forPort(serverPort)
        server = builder.build().start()

        thread { server.awaitTermination() }

        LOG.info("gRPC server running on port ${serverPort}")
        applContext.publishEvent(ApplicationStartupEvent(this))
        running.set(true)
        return this
    }

    @Synchronized
    override fun stop(): GrpcServer {
        if (isRunning && running.compareAndSet(true, false)) {
            try {
                server.shutdown()
                if (applContext.isRunning) {
                    applContext.stop()
                    applContext.publishEvent(ApplicationShutdownEvent(this))
                }
            } catch (e: Throwable) {
                if (LOG.isErrorEnabled) {
                    LOG.error("Error stopping Micronaut gRPC server: " + e.message, e)
                }
            }
        }
        return this
    }
}
