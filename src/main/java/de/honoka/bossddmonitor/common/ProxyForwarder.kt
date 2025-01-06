package de.honoka.bossddmonitor.common

import de.honoka.bossddmonitor.config.MainProperties
import de.honoka.sdk.util.kotlin.net.socket.SocketForwarder
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.io.Closeable

@Component
class ProxyForwarder(mainProperties: MainProperties) : Closeable {
    
    val forwarder = mainProperties.proxy?.let { SocketForwarder(setOf(it)) }
    
    @PreDestroy
    override fun close() {
        forwarder?.close()
    }
}
