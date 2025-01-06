package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.config.MonitorProperties
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.platform.Platform
import de.honoka.qqrobot.starter.component.ExceptionReporter
import de.honoka.sdk.util.kotlin.concurrent.ScheduledTask
import org.springframework.stereotype.Service
import java.util.concurrent.RejectedExecutionException

@Service
class MonitorService(
    private val monitorProperties: MonitorProperties,
    private val subscriptionService: SubscriptionService,
    private val jobPushRecordService: JobPushRecordService,
    private val exceptionReporter: ExceptionReporter,
    private val platforms: List<Platform>
) {
    
    val scheduledTask = run {
        ScheduledTask(monitorProperties.delay, monitorProperties.initialDelay) {
            doTask()
        }.apply {
            exceptionCallback = {
                exceptionReporter.report(it)
            }
        }
    }
    
    private fun doTask() {
        subscriptionService.list().forEach {
            platforms.forEach { p ->
                if(ServiceLauncher.appShutdown) return
                runCatching {
                    doDataExtracting(it, p)
                    jobPushRecordService.scanAndCreateMissingRecords(it)
                }.getOrElse {
                    exceptionReporter.report(it)
                }
            }
        }
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
