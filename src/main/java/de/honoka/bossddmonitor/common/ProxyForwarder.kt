package de.honoka.bossddmonitor.common

import de.honoka.bossddmonitor.config.MainProperties
import de.honoka.sdk.util.kotlin.net.socket.SocketForwarder
import org.springframework.stereotype.Component

@Component
class ProxyForwarder(mainProperties: MainProperties) {
    
    val forwarder = mainProperties.proxy?.let { SocketForwarder(setOf(it)) }
}
