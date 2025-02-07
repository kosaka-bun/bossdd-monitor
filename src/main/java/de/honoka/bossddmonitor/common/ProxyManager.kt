package de.honoka.bossddmonitor.common

import de.honoka.bossddmonitor.config.MainProperties
import de.honoka.sdk.util.kotlin.basic.exception
import de.honoka.sdk.util.kotlin.net.socket.SocketUtils
import jakarta.annotation.PreDestroy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.proxy.auth.AuthType
import org.springframework.stereotype.Component
import java.io.Closeable

@Component
class ProxyManager(private val mainProperties: MainProperties) : Closeable {

    val available = mainProperties.proxy.address != null

    @Volatile
    private var proxyOrNull: BrowserMobProxyServer? = null

    val proxy: BrowserMobProxyServer
        get() = run {
            if(!available || ServiceLauncher.appShutdown) {
                exception("Cannot get proxy.")
            }
            proxyOrNull ?: synchronized(this) {
                proxyOrNull ?: newProxy()
                proxyOrNull!!
            }
        }

    @Synchronized
    fun newProxy() {
        if(!available || ServiceLauncher.appShutdown) return
        close()
        proxyOrNull = BrowserMobProxyServer().apply {
            chainedProxy = SocketUtils.parseInetSocketAddress(mainProperties.proxy.address!!)
            mainProperties.proxy.usernameWithSession?.let {
                chainedProxyAuthorization(it, mainProperties.proxy.password, AuthType.BASIC)
            }
            start(mainProperties.proxy.localPort)
        }
    }

    @PreDestroy
    override fun close() {
        runCatching {
            proxyOrNull?.abort()
        }
        proxyOrNull = null
    }
}
