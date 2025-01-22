package de.honoka.bossddmonitor.config

import de.honoka.sdk.util.file.FileUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Paths

@ConfigurationProperties("app.browser")
data class BrowserProperties(
    
    var executablePath: String? = null,
    
    var startProcessByApp: Boolean = false,
    
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
