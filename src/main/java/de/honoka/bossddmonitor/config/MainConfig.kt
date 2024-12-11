package de.honoka.bossddmonitor.config

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.config.property.BossddProperties
import de.honoka.bossddmonitor.config.property.BrowserProperties
import de.honoka.sdk.util.kotlin.code.log
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@EnableConfigurationProperties(value = [
    BrowserProperties::class, BossddProperties::class
])
@Configuration
class MainConfig {
    
    @PreDestroy
    fun beforeExit() {
        GlobalComponents.scheduledExecutor.run {
            shutdown()
            awaitTermination(5, TimeUnit.SECONDS)
        }
        log.info("Application has been closed.")
    }
}