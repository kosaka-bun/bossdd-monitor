package de.honoka.bossddmonitor.service

import cn.hutool.core.util.ObjectUtil
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONObject
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.common.ProxyForwarder
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.mapper.JobInfoMapper
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.qqrobot.starter.component.ExceptionReporter
import de.honoka.sdk.util.kotlin.basic.cast
import de.honoka.sdk.util.kotlin.basic.exception
import de.honoka.sdk.util.kotlin.basic.tryBlockNullable
import de.honoka.sdk.util.kotlin.net.http.browserApiHeaders
import de.honoka.sdk.util.kotlin.text.singleLine
import de.honoka.sdk.util.kotlin.text.toJsonArray
import de.honoka.sdk.util.kotlin.text.toJsonWrapper
import org.springframework.stereotype.Service

@Service
class JobInfoService(
    private val proxyForwarder: ProxyForwarder,
    private val exceptionReporter: ExceptionReporter
) : ServiceImpl<JobInfoMapper, JobInfo>() {
    
    fun isEligible(jobInfo: JobInfo, subscription: Subscription): Boolean {
        if(jobInfo.cityCode != subscription.cityCode) return false
        if(!isHrLivenessValid(jobInfo)) return false
        val minCompanyScale = jobInfo.minCompanyScale
        if(!ObjectUtil.hasNull(minCompanyScale, subscription.minCompanyScale)) {
            if(minCompanyScale!! < subscription.minCompanyScale!!) {
                return false
            }
        }
        val averageSalary = jobInfo.averageSalary
        if(!ObjectUtil.hasNull(averageSalary, subscription.minSalary)) {
            if(averageSalary!! < subscription.minSalary!!) {
                return false
            }
        }
        val minExperience = jobInfo.minExperience
        if(!ObjectUtil.hasNull(minExperience, subscription.maxExperience)) {
            if(minExperience!! > subscription.maxExperience!!) {
                return false
            }
        }
        return !hasBlockWords(jobInfo, subscription) && isRelatedSearchWord(jobInfo, subscription)
    }
    
    private fun hasBlockWords(jobInfo: JobInfo, subscription: Subscription): Boolean = subscription.run {
        val propertiesToCheck = jobInfo.run {
            listOf(title, company, companyFullName, tags, details, address)
        }
        blockWords?.toJsonArray()?.forEach {
            propertiesToCheck.firstOrNull { s ->
                s?.lowercase()?.contains(it.cast<String>().lowercase()) == true
            }?.let {
                return true
            }
        }
        blockRegexes?.toJsonArray()?.forEach {
            propertiesToCheck.firstOrNull { s ->
                s?.contains(Regex(it as String, RegexOption.IGNORE_CASE)) == true
            }?.let {
                return true
            }
        }
        return false
    }
    
    private fun isRelatedSearchWord(jobInfo: JobInfo, subscription: Subscription): Boolean {
        if(ObjectUtil.hasNull(jobInfo.fromSearchWord, subscription.searchWord)) return false
        val fromSearchWord = jobInfo.fromSearchWord!!.lowercase()
        val lowerSearchWord = subscription.searchWord!!.lowercase()
        return fromSearchWord.contains(lowerSearchWord) || lowerSearchWord.contains(fromSearchWord)
    }
    
    fun isHrLivenessValid(jobInfo: JobInfo): Boolean {
        val validLivenessList = when(jobInfo.platform) {
            PlatformEnum.BOSSDD -> listOf("在线", "刚刚活跃", "今日活跃", "昨日活跃")
            else -> exception("Not support the platform: ${jobInfo.platform}")
        }
        jobInfo.hrLiveness?.let {
            return it in validLivenessList
        }
        return true
    }
    
    fun getCommutingDuration(jobInfo: JobInfo, subscription: Subscription): Int? = run {
        runCatching {
            tryBlockNullable(3) {
                doGetCommutingDuration(jobInfo, subscription)
            }
        }.getOrElse {
            exceptionReporter.report(it)
            throw it
        }
    }
    
    private fun doGetCommutingDuration(jobInfo: JobInfo, subscription: Subscription): Int? {
        subscription.maxCommutingDuration ?: return null
        when(jobInfo.platform) {
            PlatformEnum.BOSSDD -> {
                val url = """
                    https://amap-proxy.zpurl.cn/_AMapService/v3/direction/transit/integrated?
                    platform=JS&s=rsv3&logversion=2.0&key=6104503ca2f1d66e900a7e7064c5d880&
                    sdkversion=2.0.6.1&city=%E5%8C%97%E4%BA%AC%E5%B8%82&strategy=&nightflag=0&
                    appname=https%253A%252F%252Fwww.zhipin.com%252Fweb%252Fgeek%252Fmap%252Fpath&
                    origin=${subscription.userGpsLocation}&destination=${jobInfo.gpsLocation}&
                    extensions=&s=rsv3&cityd=NaN
                """.singleLine()
                val res = HttpUtil.createGet(url).run {
                    browserApiHeaders()
                    proxyForwarder.forwarder?.run {
                        closeAllConnections()
                        setHttpProxy("localhost", port)
                    }
                    execute().body().toJsonWrapper()
                }
                if(res.getStr("status") != "1") {
                    exception("Response info: ${res.getStrOrNull("info")}")
                }
                val minDuration = res.getArray("route.transits").minOfOrNull {
                    it.cast<JSONObject>().getStr("duration").toInt()
                }
                return minDuration?.let { it / 60 }
            }
            else -> exception("Not support the platform: ${jobInfo.platform}")
        }
    }
}
