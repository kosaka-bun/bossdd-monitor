package de.honoka.bossddmonitor.config

import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.sdk.spring.starter.core.context.springBean
import de.honoka.sdk.util.kotlin.basic.log
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(value = [
    MainProperties::class,
    BrowserProperties::class,
    MonitorProperties::class
])
@Configuration
class MainConfig {
    
    @PostConstruct
    fun onStarting() {
        System.setProperty("java.awt.headless", "false")
    }
    
    @PreDestroy
    fun beforeExit() {
        ServiceLauncher::class.springBean.stop()
        log.info("Application has been closed.")
    }
}

@ConfigurationProperties("app")
data class MainProperties(
    
    var proxy: String? = null
)
