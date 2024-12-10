package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.config.property.BrowserProperties
import de.honoka.sdk.util.kotlin.code.exception
import de.honoka.sdk.util.kotlin.code.tryBlock
import jakarta.annotation.PostConstruct
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
    
    @Synchronized
    fun initBrowser(headless: Boolean = true) {
        closeBrowser()
        val options = ChromeOptions().apply {
            addArguments("--user-data-dir=${browserProperties.userDataDir.absolutePath}")
            if(headless) addArguments("--headless")
        }
        browserOrNull = ChromeDriver(options)
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
    }
    
    @Synchronized
    fun clearUserDataDir() {
        val dir = File(browserProperties.userDataDir.absolutePath)
        if(dir.exists()) dir.deleteRecursively()
    }
    
    @Synchronized
    fun waitForResponse(
        urlToLoad: String, urlPrefixToWait: String, resultPredicate: ((String) -> Boolean)? = null
    ): String {
        browserOrNull ?: initBrowser()
        browser.get("about:blank")
        TimeUnit.MILLISECONDS.sleep(500)
        tryBlock(3) {
            val future = executor.submit(Callable {
                var result: String? = null
                val resultList = Collections.synchronizedList(LinkedList<String>())
                urlPrefixToResponseMap[urlPrefixToWait] = resultList
                try {
                    browser.get(urlToLoad)
                    outer@
                    for(i in 1..20) {
                        TimeUnit.MILLISECONDS.sleep(500)
                        if(resultList.isEmpty()) continue
                        for(r in resultList) {
                            if(resultPredicate == null || resultPredicate(r)) {
                                result = r
                                break@outer
                            }
                        }
                    }
                } finally {
                    urlPrefixToResponseMap.remove(urlPrefixToWait)
                }
                result ?: exception("Cannot get the response of $urlPrefixToWait")
            })
            return future.get()
        }
    }
    
    fun handleResponse(devTools: DevTools, event: ResponseReceived) {
        val url = event.response.url
        val response = devTools.send(Network.getResponseBody(event.requestId)).body
        urlPrefixToResponseMap.forEach { (k, v) ->
            if(url.startsWith(k)) v.add(response)
        }
    }
    
    private fun moveBrowserToCenter() = browser.manage().window().run {
        size = Dimension(1280, 850)
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val left = (screenSize.width - size.width) / 2
        val top = (screenSize.height - size.height) / 2
        position = Point(left, top)
    }
}