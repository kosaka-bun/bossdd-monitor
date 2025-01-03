package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.config.MonitorProperties
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.platform.Platform
import org.springframework.stereotype.Service
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Service
class MonitorService(
    private val monitorProperties: MonitorProperties,
    private val subscriptionService: SubscriptionService,
    private val jobPushRecordService: JobPushRecordService,
    private val exceptionReportService: ExceptionReportService,
    private val platforms: List<Platform>
) {
    
    @Volatile
    private var runningTask: ScheduledFuture<*>? = null
    
    @Synchronized
    fun startup() {
        stop()
        val action = {
            runCatching {
                doTask()
            }.getOrElse {
                exceptionReportService.report(it)
            }
        }
        runningTask = GlobalComponents.scheduledExecutor.scheduleWithFixedDelay(
            action,
            Duration.parse(monitorProperties.initialDelay).inWholeMilliseconds,
            Duration.parse(monitorProperties.delay).inWholeMilliseconds,
            TimeUnit.MILLISECONDS
        )
    }
    
    @Synchronized
    fun stop() {
        runningTask?.run {
            cancel(true)
            runningTask = null
        }
    }
    
    private fun doTask() {
        subscriptionService.list().forEach {
            runCatching {
                platforms.forEach { p ->
                    doDataExtracting(it, p)
                }
            }.getOrElse {
                exceptionReportService.report(it)
            }
        }
    }
    
    private fun doDataExtracting(subscription: Subscription, platform: Platform) {
        runCatching {
            platform.doDataExtracting(subscription)
            jobPushRecordService.scanJobListAndCreateNewPushRecords(subscription)
        }.getOrElse {
            exceptionReportService.report(it)
        }
    }
}
