package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.sdk.spring.starter.mybatis.queryChainWrapper
import org.apache.ibatis.annotations.Mapper

@Mapper
interface JobPushRecordMapper : BaseMapper<JobPushRecord> {
    
    fun getFirstNotPushedRecord(userId: Long): JobPushRecord? {
        queryChainWrapper().run {
            eq(JobPushRecord::subscribeUserId, userId)
            eq(JobPushRecord::pushed, false)
            eq(JobPushRecord::valid, true)
            last("limit 1")
            return one()
        }
    }
}
