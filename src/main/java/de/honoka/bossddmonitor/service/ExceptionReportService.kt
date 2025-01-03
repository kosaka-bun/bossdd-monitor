package de.honoka.bossddmonitor.service

import de.honoka.sdk.util.kotlin.basic.log
import org.springframework.stereotype.Service

@Service
class ExceptionReportService {
    
    fun report(t: Throwable) {
        log.error("", t)
    }
}
