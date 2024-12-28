package de.honoka.bossddmonitor.config.property

import de.honoka.sdk.util.file.FileUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Paths

@ConfigurationProperties("app.browser")
class BrowserProperties(
    
    var userDataDir: UserDataDir = UserDataDir(),
    
    var proxy: String? = null
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
}
