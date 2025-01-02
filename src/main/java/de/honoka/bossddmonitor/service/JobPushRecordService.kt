package de.honoka.bossddmonitor.service

import cn.hutool.core.util.ObjectUtil
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
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
    
    fun scanJobListAndCreateNewPushRecords(subscription: Subscription) {
        jobInfoService.baseMapper.getNoRecordsJobInfoList(subscription.userId!!).forEach {
            runCatching {
                checkAndCreate(it, subscription)
            }
        }
    }
    
    fun checkAndCreate(jobInfo: JobInfo) {
        subscriptionService.list().forEachCatching {
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
            val isInvalid = jobInfo.let {
                !isEligible(it, subscription) || !hasKeyword(it, subscription.searchWord!!)
            }
            if(isInvalid) {
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
