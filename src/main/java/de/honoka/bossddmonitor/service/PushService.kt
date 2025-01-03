package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.GlobalComponents
import org.springframework.stereotype.Service
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class PushService {
    
    @Volatile
    private var runningTask: ScheduledFuture<*>? = null
    
    @Synchronized
    fun startup() {
        stop()
        val action: () -> Unit = {
            runCatching {
                doTask()
            }
        }
        runningTask = GlobalComponents.scheduledExecutor.scheduleWithFixedDelay(
            action, 1, 1, TimeUnit.MINUTES
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
    
    }
}
