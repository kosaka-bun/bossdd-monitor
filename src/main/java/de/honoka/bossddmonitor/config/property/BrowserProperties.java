package de.honoka.bossddmonitor.config.property;

import de.honoka.sdk.util.file.FileUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Paths;

@ConfigurationProperties("app.browser")
@Data
public class BrowserProperties {
    
    private UserDataDir userDataDir = new UserDataDir();
    
    @Data
    public static class UserDataDir {
        
        private String path = "./selenium/user-data";
        
        private boolean clearOnStartup = false;
        
        public String getAbsolutePath() {
            return Paths.get(FileUtils.getMainClasspath(), path).toAbsolutePath().normalize().toString();
        }
    }
}
