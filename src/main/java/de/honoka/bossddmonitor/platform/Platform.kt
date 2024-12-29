package de.honoka.bossddmonitor.platform

import de.honoka.bossddmonitor.entity.Subscription

interface Platform {
    
    fun doDataExtracting(subscription: Subscription)
}