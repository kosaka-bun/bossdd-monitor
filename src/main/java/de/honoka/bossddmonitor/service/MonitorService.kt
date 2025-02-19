package de.honoka.bossddmonitor.service

import cn.hutool.core.date.DateTime
import de.honoka.bossddmonitor.common.ExtendedExceptionReporter
import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.config.MonitorProperties
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.platform.Platform
import de.honoka.sdk.util.kotlin.basic.weekdayNum
import de.honoka.sdk.util.kotlin.concurrent.ScheduledTask
import org.springframework.stereotype.Service
import java.util.concurrent.RejectedExecutionException

@Service
class MonitorService(
    private val monitorProperties: MonitorProperties,
    private val subscriptionService: SubscriptionService,
    private val jobPushRecordService: JobPushRecordService,
    private val exceptionReporter: ExtendedExceptionReporter,
    private val platforms: List<Platform>
) {
    
    val scheduledTask = ScheduledTask(
        monitorProperties.delay,
        monitorProperties.initialDelay,
        action = ::doTask
    ).apply {
        exceptionCallback = {
            exceptionReporter.report(it)
        }
    }
    
    private fun doTask() {
        if(!isCurrentTimeInRange()) return
        subscriptionService.list().forEach {
            if(!it.enabled!!) return@forEach
            platforms.forEach { p ->
                if(ServiceLauncher.appShutdown) return
                runCatching {
                    doDataExtracting(it, p)
                    jobPushRecordService.scanAndCreateMissingRecords(it)
                }.getOrElse { t ->
                    exceptionReporter.report(t)
                }
            }
        }
    }
    
    private fun isCurrentTimeInRange(): Boolean {
        val now = DateTime.now()
        now.hour(true).let {
            val range = monitorProperties.hourRangeParts
            if(it < range[0] || it >= range[1]) return false
        }
        now.weekdayNum.let {
            val range = monitorProperties.weekdayRangeParts
            if(it < range[0] || it > range[1]) return false
        }
        return true
    }
    
    private fun doDataExtracting(subscription: Subscription, platform: Platform) {
        runCatching {
            platform.doDataExtracting(subscription)
        }.getOrElse {
            if(it is RejectedExecutionException) return
            exceptionReporter.report(it)
        }
    }
}
