package de.honoka.bossddmonitor.common

import de.honoka.sdk.util.kotlin.concurrent.ThreadPoolUtilsExt

object GlobalComponents {
    
    val scheduledExecutor = ThreadPoolUtilsExt.newScheduledPool(2)
}
