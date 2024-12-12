package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.config.property.BrowserProperties
import de.honoka.sdk.util.kotlin.code.exception
import de.honoka.sdk.util.kotlin.code.log
import de.honoka.sdk.util.kotlin.code.tryBlock
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.intellij.lang.annotations.Language
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.devtools.Connection
import org.openqa.selenium.devtools.DevTools
import org.openqa.selenium.devtools.v85.network.Network
import org.openqa.selenium.devtools.v85.network.model.ResponseReceived
import org.springframework.stereotype.Service
import java.awt.Toolkit
import java.io.File
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy
import java.util.logging.Level
import java.util.logging.Logger

@Service
class BrowserService(private val browserProperties: BrowserProperties) {

    private var browserOrNull: ChromeDriver? = null
    
    val browser: ChromeDriver
        get() = browserOrNull!!
    
    private val executor = ThreadPoolExecutor(
        1, 10, 60, TimeUnit.SECONDS,
        SynchronousQueue(), AbortPolicy()
    )
    
    private val urlPrefixToResponseMap = ConcurrentHashMap<String, MutableList<String>>()
    
    @PostConstruct
    private fun init() {
        disableSeleniumLog()
        if(browserProperties.userDataDir.isClearOnStartup) {
            clearUserDataDir()
        }
    }
    
    @PreDestroy
    private fun stop() = closeBrowser()
    
    private fun disableSeleniumLog() {
        val classes = listOf(
            DevTools::class,
            Connection::class
        )
        classes.forEach {
            val logger = it.java.getDeclaredField("LOG").run {
                isAccessible = true
                get(null) as Logger
            }
            logger.level = Level.OFF
        }
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
    }
    
    private fun moveBrowserToCenter() = browser.manage().window().run {
        size = Dimension(1280, 850)
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val left = (screenSize.width - size.width) / 2
        val top = (screenSize.height - size.height) / 2
        position = Point(left, top)
    }
    
    private fun handleResponse(devTools: DevTools, event: ResponseReceived) {
        val url = event.response.url
        val response = devTools.send(Network.getResponseBody(event.requestId)).body
        urlPrefixToResponseMap.forEach { (k, v) ->
            if(url.startsWith(k)) v.add(response)
        }
    }
    
    @Synchronized
    fun initBrowser(headless: Boolean = true) {
        closeBrowser()
        val options = ChromeOptions().apply {
            val userDataDir = browserProperties.userDataDir.absolutePath
            log.info("Used user data directory of Selenium Chrome driver: $userDataDir")
            addArguments("--user-data-dir=$userDataDir")
            if(headless) addArguments("--headless")
        }
        browserOrNull = ChromeDriver(options)
        browser.setLogLevel(Level.OFF)
        if(!headless) moveBrowserToCenter()
        browser.devTools.run {
            createSession()
            send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()))
            addListener(Network.responseReceived()) { e ->
                runCatching {
                    handleResponse(this, e)
                }
            }
        }
    }
    
    @Synchronized
    fun closeBrowser() {
        browserOrNull ?: return
        browser.run {
            devTools.close()
            quit()
        }
        browserOrNull = null
        log.info("Selenium Chrome driver has been closed.")
    }
    
    @Synchronized
    fun clearUserDataDir() {
        val dir = File(browserProperties.userDataDir.absolutePath)
        if(dir.exists()) dir.deleteRecursively()
    }
    
    @Synchronized
    fun loadBlankPage(waitMillisAfterLoad: Long = 0) {
        browserOrNull ?: initBrowser()
        browser.get("about:blank")
        TimeUnit.MILLISECONDS.sleep(waitMillisAfterLoad)
    }
    
    @Synchronized
    fun loadPage(url: String) {
        loadBlankPage(500)
        browser.get(url)
    }
    
    @Synchronized
    fun waitForResponse(
        urlToLoad: String, urlPrefixToWait: String, resultPredicate: ((String) -> Boolean)? = null
    ): String {
        loadBlankPage(500)
        tryBlock(3) {
            val resultList = Collections.synchronizedList(LinkedList<String>())
            val action = Callable {
                var result: String? = null
                browser.get(urlToLoad)
                outer@
                for(i in 1..20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                    if(resultList.isEmpty()) continue
                    for(r in resultList) {
                        val shouldTake = resultPredicate == null || runCatching {
                            resultPredicate(r)
                        }.getOrDefault(false)
                        if(shouldTake) {
                            result = r
                            break@outer
                        }
                    }
                }
                result ?: exception("Cannot get the response of $urlPrefixToWait")
            }
            try {
                urlPrefixToResponseMap[urlPrefixToWait] = resultList
                return executor.submit(action).get(10, TimeUnit.SECONDS)
            } finally {
                urlPrefixToResponseMap.remove(urlPrefixToWait)
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> executeJsExpression(@Language("JavaScript") jsExpression: String): T? = run {
        browser.executeScript("return $jsExpression") as T?
    }
    
    @Synchronized
    fun <T> waitForJsResultOrNull(
        urlToLoad: String,
        @Language("JavaScript") jsExpression: String,
        resultPredicate: ((T?) -> Boolean)? = null,
        continueWaitOnResultIsNull: Boolean = false
    ): T? {
        loadBlankPage(500)
        tryBlock(3) {
            val future = executor.submit(Callable {
                var result: T? = null
                var hasResult = false
                browser.get(urlToLoad)
                for(i in 1..20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                    try {
                        val r = executeJsExpression<T>(jsExpression)
                        if(r == null && continueWaitOnResultIsNull) continue
                        val shouldTake = resultPredicate == null || runCatching {
                            resultPredicate(r)
                        }.getOrDefault(false)
                        if(shouldTake) {
                            result = r
                            hasResult = true
                            break
                        }
                    } catch(t: Throwable) {
                        //ignore
                    }
                }
                if(!hasResult) error("Cannot get the result of JavaScript expression: $jsExpression")
                result
            })
            return future.get(10, TimeUnit.SECONDS)
        }
    }
    
    @Synchronized
    fun <T> waitForJsResult(
        urlToLoad: String,
        @Language("JavaScript") jsExpression: String,
        resultPredicate: ((T) -> Boolean)? = null,
        continueWaitOnResultIsNull: Boolean = true
    ): T = run {
        var newResultPredicate: ((T?) -> Boolean)? = null
        resultPredicate?.let { p ->
            newResultPredicate = { p(it!!) }
        }
        waitForJsResultOrNull(urlToLoad, jsExpression, newResultPredicate, continueWaitOnResultIsNull)!!
    }
    
    fun checkIsActive() {
        browser.run {
            //尝试获取以下属性的值，若无法获取将抛出异常，可视为浏览器已关闭
            windowHandle
            currentUrl
            title
        }
    }
}