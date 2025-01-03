package de.honoka.bossddmonitor.common

import de.honoka.bossddmonitor.service.MonitorService
import de.honoka.bossddmonitor.service.PushService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ServiceLauncher(
    private val monitorService: MonitorService,
    private val pushService: PushService
) : ApplicationRunner {
    
    override fun run(args: ApplicationArguments?) {
        monitorService.startup()
        pushService.startup()
    }
}
