package de.honoka.bossddmonitor.service

import cn.hutool.core.util.ObjectUtil
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.mapper.JobPushRecordMapper
import de.honoka.sdk.util.kotlin.basic.forEachCatching
import org.springframework.stereotype.Service

@Service
class JobPushRecordService(
    private val subscriptionService: SubscriptionService,
    private val jobInfoService: JobInfoService
) : ServiceImpl<JobPushRecordMapper, JobPushRecord>() {
    
    fun scanAndCreateMissingRecords(subscription: Subscription) {
        if(ServiceLauncher.appShutdown || !subscription.enabled!!) return
        jobInfoService.baseMapper.getNoRecordsJobInfoList(subscription.userId!!).forEachCatching {
            if(ServiceLauncher.appShutdown) return
            checkAndCreate(it, subscription)
        }
    }
    
    fun checkAndCreate(jobInfo: JobInfo) {
        subscriptionService.list().forEachCatching {
            if(!it.enabled!!) return@forEachCatching
            checkAndCreate(jobInfo, it)
        }
    }
    
    private fun checkAndCreate(jobInfo: JobInfo, subscription: Subscription) {
        val record = JobPushRecord().apply {
            jobInfoId = jobInfo.id
            subscribeUserId = subscription.userId
            userGpsLocation = subscription.userGpsLocation
            pushed = false
            valid = true
        }
        jobInfoService.run {
            if(!isEligible(jobInfo, subscription)) {
                record.valid = false
                save(record)
                return
            }
            record.commuteDuration = getCommutingDuration(jobInfo, subscription)
        }
        if(!ObjectUtil.hasNull(record.commuteDuration, subscription.maxCommutingDuration)) {
            if(record.commuteDuration!! > subscription.maxCommutingDuration!!) {
                record.valid = false
                save(record)
                return
            }
        }
        save(record)
    }
}
