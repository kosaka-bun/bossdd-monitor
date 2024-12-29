package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.config.DataServiceProperties
import de.honoka.bossddmonitor.platform.Platform
import jakarta.annotation.PreDestroy
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Service
class MonitorService(
    private val dataServiceProperties: DataServiceProperties,
    private val subscriptionService: SubscriptionService,
    private val exceptionReportService: ExceptionReportService,
    private val platforms: List<Platform>
) : ApplicationRunner {
    
    @Volatile
    private var runningTask: ScheduledFuture<*>? = null
    
    override fun run(args: ApplicationArguments) {
        startup()
    }
    
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
            Duration.parse(dataServiceProperties.monitorTask.initialDelay).inWholeMilliseconds,
            Duration.parse(dataServiceProperties.monitorTask.delay).inWholeMilliseconds,
            TimeUnit.MILLISECONDS
        )
    }
    
    @PreDestroy
    @Synchronized
    fun stop() {
        runningTask?.run {
            cancel(true)
            runningTask = null
        }
    }
    
    private fun doTask() {
        subscriptionService.list().forEach {
            platforms.forEach { p ->
                runCatching {
                    p.doDataExtracting(it)
                }.getOrElse {
                    exceptionReportService.report(it)
                }
            }
        }
    }
}
