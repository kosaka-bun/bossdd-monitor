package de.honoka.bossddmonitor.service

import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.qqrobot.framework.api.RobotFramework
import de.honoka.qqrobot.framework.api.model.RobotMessageType
import de.honoka.qqrobot.framework.api.model.RobotMultipartMessage
import de.honoka.sdk.util.kotlin.basic.log
import de.honoka.sdk.util.kotlin.concurrent.ScheduledTask
import de.honoka.sdk.util.kotlin.text.singleLine
import de.honoka.sdk.util.kotlin.text.toJsonObject
import de.honoka.sdk.util.kotlin.text.trimAllLines
import de.honoka.sdk.util.various.ImageUtils
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class PushService(
    private val subscriptionService: SubscriptionService,
    private val jobInfoService: JobInfoService,
    private val jobPushRecordService: JobPushRecordService,
    private val robotFramework: RobotFramework
) {
    
    val scheduledTask = ScheduledTask("1m", "1m", action = ::doTask)
    
    private fun doTask() {
        subscriptionService.list().forEach {
            if(ServiceLauncher.appShutdown) return
            runCatching {
                pushJobInfo(it)
            }.getOrElse {
                log.error("", it)
            }
        }
    }
    
    private fun pushJobInfo(subscription: Subscription) {
        val record = jobPushRecordService.baseMapper.getFirstNotPushedRecord(subscription.userId!!)
        record ?: return
        val jobInfo = jobInfoService.getById(record.jobInfoId) ?: return
        val message = RobotMultipartMessage().apply {
            add(RobotMessageType.IMAGE, getImageToPush(jobInfo, record))
            add(RobotMessageType.TEXT, getUrlToPush(jobInfo))
        }
        if(subscription.receiverGroupId != null) {
            robotFramework.sendGroupMsg(subscription.receiverGroupId!!, message)
        } else {
            robotFramework.sendPrivateMsg(subscription.userId!!, message)
        }
        jobPushRecordService.updateById(JobPushRecord().apply {
            id = record.id
            pushed = true
        })
    }
    
    private fun getImageToPush(jobInfo: JobInfo, jobPushRecord: JobPushRecord): InputStream {
        val text = jobInfo.run {
            """
                【${company}】 - $title <b>$salary</b>
                公司全名：$companyFullName
                规模：$companyScale
                HR：$hrName
                HR活跃度：$hrLiveness
                经验要求：$experience
                岗位地址：$address
                通勤时间：${jobPushRecord.commuteDuration}分钟
                
                $details
            """
        }.trimAllLines()
        return ImageUtils.textToImageByLength(text, 60)
    }
    
    private fun getUrlToPush(jobInfo: JobInfo): String = when(jobInfo.platform) {
        PlatformEnum.BOSSDD -> jobInfo.run {
            val identifiersMap = identifiers!!.toJsonObject()
            """
                https://www.zhipin.com/job_detail/$platformJobId.html?
                lid=${identifiersMap["lid"]}&
                securityId=${identifiersMap["securityId"]}
            """.singleLine()
        }
        else -> ""
    }
}
