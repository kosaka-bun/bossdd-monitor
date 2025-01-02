package de.honoka.bossddmonitor.service

import cn.hutool.core.util.ObjectUtil
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.mapper.JobPushRecordMapper
import org.springframework.stereotype.Service

@Service
class JobPushRecordService : ServiceImpl<JobPushRecordMapper, JobPushRecord>() {
    
    fun create(jobInfo: JobInfo, subscription: Subscription) {
        val record = JobPushRecord().apply {
            jobInfoId = jobInfo.id
            subscribeUserId = subscription.userId
            userGpsLocation = subscription.userGpsLocation
            commuteDuration = jobInfo.getCommutingDuration(subscription)
            pushed = false
        }
        if(!ObjectUtil.hasNull(record.commuteDuration, subscription.maxCommutingDuration)) {
            if(record.commuteDuration!! > subscription.maxCommutingDuration!!) return
        }
        save(record)
    }
}
