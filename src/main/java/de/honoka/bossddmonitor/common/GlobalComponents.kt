package de.honoka.bossddmonitor.common

import de.honoka.sdk.spring.starter.core.context.ApplicationContextHolder.springBean
import de.honoka.sdk.util.kotlin.concurrent.ThreadPoolUtilsExt

object GlobalComponents {
    
    val scheduledExecutor = ThreadPoolUtilsExt.newScheduledPool(2)
    
    val proxyForwarder = ProxyForwarder::class.springBean
}
