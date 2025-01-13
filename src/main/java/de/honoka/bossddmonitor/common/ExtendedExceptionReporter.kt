package de.honoka.bossddmonitor.common

import cn.hutool.core.bean.BeanUtil
import cn.hutool.core.exceptions.ExceptionUtil
import cn.hutool.json.JSONUtil
import de.honoka.bossddmonitor.service.BrowserService
import de.honoka.qqrobot.framework.ExtendedRobotFramework
import de.honoka.qqrobot.framework.api.model.RobotMessage
import de.honoka.qqrobot.framework.api.model.RobotMultipartMessage
import de.honoka.qqrobot.starter.component.ExceptionReporter
import de.honoka.sdk.util.kotlin.basic.cast
import de.honoka.sdk.util.kotlin.basic.log
import de.honoka.sdk.util.kotlin.concurrent.ScheduledTask
import de.honoka.sdk.util.various.ImageUtils
import org.springframework.stereotype.Component
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

@Component
class ExtendedExceptionReporter(
    private val exceptionReporter: ExceptionReporter,
    private val robotFramework: ExtendedRobotFramework
) {
    
    private data class ExceptionCounts(
        
        var waitForResponseTimeout: AtomicInteger = AtomicInteger(0),
        
        val onErrorPage: AtomicInteger = AtomicInteger(0)
    )
    
    private val scheduledTask = ScheduledTask("1h", "10m", action = ::doTask)
    
    private val counts = ExceptionCounts()
    
    init {
        scheduledTask.startup()
    }
    
    private fun doTask() {
        val map = BeanUtil.beanToMap(counts).apply {
            if(values.all { it.cast<AtomicInteger>().get() < 1 }) return
        }
        val json = JSONUtil.toJsonPrettyStr(map)
        setAllCountsToZero(map.values)
        robotFramework.sendMsgToDevelopingGroup(RobotMultipartMessage().apply {
            add(RobotMessage.text("过去1小时内受计数异常产生次数："))
            add(RobotMessage.image(ImageUtils.textToImageByLength(json, 50)))
        })
    }
    
    private fun setAllCountsToZero(countValues: Collection<Any>) {
        countValues.forEach {
            it.cast<AtomicInteger>().set(0)
        }
    }

    fun report(t: Throwable) {
        val blocked = when(t) {
            is TimeoutException -> checkException(t)
            is BrowserService.OnErrorPageException -> {
                counts.onErrorPage.incrementAndGet()
                true
            }
            else -> false
        }
        if(blocked) {
            log.error("", t)
        } else {
            exceptionReporter.report(t)
        }
    }
    
    private fun checkException(e: TimeoutException): Boolean = run {
        ExceptionUtil.stacktraceToString(e).run {
            when {
                contains("BrowserService.waitForResponse") -> {
                    counts.waitForResponseTimeout.incrementAndGet()
                    true
                }
                else -> false
            }
        }
    }
}
