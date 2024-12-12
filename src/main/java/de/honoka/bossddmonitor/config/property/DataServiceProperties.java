package de.honoka.bossddmonitor.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.data-service")
@Data
public class DataServiceProperties {

    private MonitorTask monitorTask = new MonitorTask();

    @Data
    public static class MonitorTask {
        
        /**
         * 任务启动后，在第一次执行监控任务前需等待的时间长度
         */
        private String initialDelay = "1m";
        
        /**
         * 任务在每次执行完成之后到下一次执行前需等待的时间长度
         */
        private String delay = "5m";
    }
}
