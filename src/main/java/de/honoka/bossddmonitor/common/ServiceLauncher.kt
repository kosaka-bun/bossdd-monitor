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
    
    override fun run(args: ApplicationArguments) {
        browserService.init()
        monitorService.startup()
        pushService.startup()
    }
    
    fun stop() {
        browserService.stop()
        monitorService.stop()
        pushService.stop()
    }
}
