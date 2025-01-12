package de.honoka.bossddmonitor.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.monitor")
data class MonitorProperties(
    
    /**
     * 任务在每次执行完成之后到下一次执行前需等待的时间长度
     */
    var delay: String = "5m",
    
    /**
     * 任务启动后，在第一次执行监控任务前需等待的时间长度
     */
    var initialDelay: String = "1m",
    
    /**
     * 一周中需要执行监控任务的日期范围（包含左右边界）
     */
    var weekdayRange: String = "1-6",
    
    /**
     * 一天内需要执行监控任务的小时范围（在左边界之后，包含左边界，右边界之前）
     */
    var hourRange: String = "8-22"
) {
    
    val weekdayRangeParts: List<Int>
        get() = weekdayRange.split("-").map { it.toInt() }
    
    val hourRangeParts: List<Int>
        get() = hourRange.split("-").map { it.toInt() }
}
