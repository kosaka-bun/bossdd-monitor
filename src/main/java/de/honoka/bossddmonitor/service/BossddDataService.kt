package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.config.property.BossddProperties
import de.honoka.sdk.spring.starter.core.web.WebUtils
import de.honoka.sdk.util.kotlin.code.log
import de.honoka.sdk.util.kotlin.text.singleLine
import jakarta.annotation.PreDestroy
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class BossddDataService(
    private val browserService: BrowserService,
    private val bossddProperties: BossddProperties
) : ApplicationRunner {
    
    private var runningTask: ScheduledFuture<*>? = null
    
    override fun run(args: ApplicationArguments) = startup()
    
    @Synchronized
    fun startup() {
        stop()
        val action = {
            try {
                doTask()
            } catch(t: Throwable) {
                log.error("", t)
            }
        }
        runningTask = GlobalComponents.scheduledExecutor.run {
            scheduleWithFixedDelay(action, 10, 60, TimeUnit.SECONDS)
        }
    }
    
    @PreDestroy
    @Synchronized
    fun stop() {
        runningTask ?: return
        runningTask?.cancel(true)
        runningTask = null
    }
    
    private fun doTask() {
        checkLogin()
    }
    
    private fun checkLogin() {
        val url = """
            https://www.zhipin.com/web/geek/job?query=${bossddProperties.searchWord}|
            &city=${bossddProperties.cityCode}|
        """.singleLine()
        repeat(2) {
            browserService.waitForJsResult<String>(url, "document.cookie").let {
                val cookie = WebUtils.cookieStringToMap(it)
                if(cookie.containsKey("bst")) return
            }
        }
        log.info("登录态已失效，请在浏览器中手动登录，浏览器将在检测到登录态后关闭")
        browserService.run {
            initBrowser(false)
            loadPage(url)
            while(true) {
                TimeUnit.SECONDS.sleep(1)
                try {
                    val cookie = executeJsExpression<String>("document.cookie")?.let {
                        WebUtils.cookieStringToMap(it)
                    }
                    if(cookie?.containsKey("bst") != true) continue
                } catch(t: Throwable) {
                    continue
                }
                initBrowser()
                break
            }
        }
    }
}