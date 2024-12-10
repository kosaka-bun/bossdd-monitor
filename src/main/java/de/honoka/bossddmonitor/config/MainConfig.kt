package de.honoka.bossddmonitor.config

import de.honoka.bossddmonitor.config.property.BrowserProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(BrowserProperties::class)
@Configuration
class MainConfig