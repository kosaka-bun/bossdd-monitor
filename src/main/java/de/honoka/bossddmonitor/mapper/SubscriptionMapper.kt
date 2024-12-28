package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.Subscription
import org.apache.ibatis.annotations.Mapper

@Mapper
interface SubscriptionMapper : BaseMapper<Subscription>
