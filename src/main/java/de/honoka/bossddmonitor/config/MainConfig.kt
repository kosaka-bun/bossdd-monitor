package de.honoka.bossddmonitor.config

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.config.property.BrowserProperties
import de.honoka.bossddmonitor.config.property.DataServiceProperties
import de.honoka.sdk.util.kotlin.basic.log
import de.honoka.sdk.util.kotlin.concurrent.shutdownNowAndWait
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(value = [
    BrowserProperties::class, DataServiceProperties::class
])
@Configuration
class MainConfig {
    
    @PostConstruct
    fun onStarting() {
        System.setProperty("java.awt.headless", "false")
    }
    
    @PreDestroy
    fun beforeExit() {
        GlobalComponents.scheduledExecutor.shutdownNowAndWait()
        log.info("Application has been closed.")
    }
}
