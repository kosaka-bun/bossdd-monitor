package de.honoka.bossddmonitor.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.mapper.SubscriptionMapper
import org.springframework.stereotype.Service

@Service
class SubscriptionService : ServiceImpl<SubscriptionMapper, Subscription>()
