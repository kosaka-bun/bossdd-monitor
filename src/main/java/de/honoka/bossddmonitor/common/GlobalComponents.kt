package de.honoka.bossddmonitor.common

import de.honoka.sdk.util.kotlin.concurrent.ThreadPoolUtils

object GlobalComponents {
    
    val scheduledExecutor = ThreadPoolUtils.newScheduledPool(2)
}
