package de.honoka.bossddmonitor.common

import de.honoka.bossddmonitor.service.BrowserService
import de.honoka.bossddmonitor.service.MonitorService
import de.honoka.bossddmonitor.service.PushService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ServiceLauncher(
    private val monitorService: MonitorService,
    private val pushService: PushService,
    private val browserService: BrowserService
) : ApplicationRunner {
    
    companion object {
        
        @Volatile
        var appShutdown = false
            private set
    }
    
    override fun run(args: ApplicationArguments) {
        browserService.init()
        monitorService.scheduledTask.startup()
        pushService.scheduledTask.startup()
    }
    
    fun stop() {
        appShutdown = true
        browserService.stop()
        monitorService.scheduledTask.close()
        pushService.scheduledTask.close()
    }
}
