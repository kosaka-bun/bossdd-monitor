package de.honoka.bossddmonitor.config

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.sdk.spring.starter.core.context.springBean
import de.honoka.sdk.util.file.FileUtils
import de.honoka.sdk.util.kotlin.basic.log
import de.honoka.sdk.util.kotlin.concurrent.shutdownNowAndWait
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

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
        GlobalComponents.scheduledExecutor.shutdownNowAndWait()
        log.info("Application has been closed.")
    }
}

@ConfigurationProperties("app")
data class MainProperties(
    
    var proxy: String? = null
)

@ConfigurationProperties("app.browser")
data class BrowserProperties(
    
    var userDataDir: UserDataDir = UserDataDir(),
    
    var defaultHeadless: Boolean = true,
    
    var blockUrlKeywords: List<String> = listOf(),
    
    var errorPageDetection: ErrorPageDetection = ErrorPageDetection()
) {
    
    data class UserDataDir(
        
        var path: String = "./selenium/user-data",
        
        var clearOnStartup: Boolean = false
    ) {
        
        val absolutePath: String
            get() {
                val pathObj = if(FileUtils.isAppRunningInJar()) {
                    Paths.get(FileUtils.getMainClasspath(), path)
                } else {
                    Paths.get(path)
                }
                return pathObj.toAbsolutePath().normalize().toString()
            }
    }
    
    data class ErrorPageDetection(
        
        var urlKeywords: List<String> = listOf(),
        
        var selectors: List<String> = listOf()
    )
}

@ConfigurationProperties("app.monitor")
data class MonitorProperties(
    
    /**
     * 任务启动后，在第一次执行监控任务前需等待的时间长度
     */
    var initialDelay: String = "1m",
    
    /**
     * 任务在每次执行完成之后到下一次执行前需等待的时间长度
     */
    var delay: String = "5m"
)
