package de.honoka.bossddmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BossddMonitorApp

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    runApplication<BossddMonitorApp>(*args)
}