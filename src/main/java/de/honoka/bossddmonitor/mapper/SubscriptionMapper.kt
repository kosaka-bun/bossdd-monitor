package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.sdk.spring.starter.mybatis.queryChainWrapper
import org.apache.ibatis.annotations.Mapper

@Mapper
interface SubscriptionMapper : BaseMapper<Subscription> {
    
    fun getByUserId(userId: Long): Subscription? {
        queryChainWrapper().run {
            eq(Subscription::userId, userId)
            last("limit 1")
            return one()
        }
    }
}
