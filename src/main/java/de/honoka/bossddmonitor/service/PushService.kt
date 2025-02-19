package de.honoka.bossddmonitor.service

import cn.hutool.core.date.DateUtil
import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.qqrobot.framework.api.RobotFramework
import de.honoka.qqrobot.framework.api.message.RobotMessage
import de.honoka.qqrobot.framework.api.message.RobotMultipartMessage
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
            if(!it.enabled!!) return@forEach
            runCatching {
                pushJobInfo(it)
            }.getOrElse { e ->
                log.error("", e)
            }
        }
    }
    
    private fun pushJobInfo(subscription: Subscription) {
        val record = jobPushRecordService.baseMapper.getFirstNotPushedRecord(subscription.userId!!)
        record ?: return
        val jobInfo = jobInfoService.getById(record.jobInfoId) ?: return
        if(!jobInfoService.isEligible(jobInfo, subscription)) {
            jobPushRecordService.updateById(JobPushRecord().apply {
                id = record.id
                valid = false
            })
            return
        }
        val message = RobotMultipartMessage().apply {
            add(RobotMessage.text("【${jobInfo.company}】${jobInfo.title}"))
            add(RobotMessage.image(getImageToPush(jobInfo, record)))
            add(RobotMessage.text(getUrlToPush(jobInfo)))
        }
        robotFramework.run {
            subscription.run {
                if(receiverGroupId != null) {
                    sendGroupMsg(receiverGroupId!!, message)
                } else {
                    sendPrivateMsg(userId!!, message)
                }
            }
        }
        jobPushRecordService.updateById(JobPushRecord().apply {
            id = record.id
            pushed = true
        })
    }
    
    private fun getImageToPush(jobInfo: JobInfo, jobPushRecord: JobPushRecord): InputStream {
        val text = jobInfo.run {
            """
                【${company}】$title
                薪资：<b>$salary</b>
                公司全名：$companyFullName
                规模：$companyScale
                HR：$hrName
                HR活跃度：$hrLiveness
                经验要求：$experience
                岗位地址：$address
                通勤时间：${jobPushRecord.commuteDuration}分钟
                信息更新时间：${DateUtil.formatDateTime(updateTime)}
                
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
