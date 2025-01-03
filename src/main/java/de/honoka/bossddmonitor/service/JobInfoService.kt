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
import de.honoka.sdk.util.kotlin.basic.cast
import de.honoka.sdk.util.kotlin.basic.exception
import de.honoka.sdk.util.kotlin.net.http.browserApiHeaders
import de.honoka.sdk.util.kotlin.text.singleLine
import de.honoka.sdk.util.kotlin.text.toJsonArray
import de.honoka.sdk.util.kotlin.text.toJsonWrapper
import org.springframework.stereotype.Service

@Service
class JobInfoService(
    private val proxyForwarder: ProxyForwarder,
    private val exceptionReportService: ExceptionReportService
) : ServiceImpl<JobInfoMapper, JobInfo>() {
    
    fun isEligible(jobInfo: JobInfo, subscription: Subscription): Boolean {
        if(jobInfo.cityCode != subscription.cityCode) return false
        val minCompanyScale = jobInfo.minCompanyScale
        if(!ObjectUtil.hasNull(minCompanyScale, subscription.minCompanyScale)) {
            if(minCompanyScale!! < subscription.minCompanyScale!!) {
                return false
            }
        }
        val minSalary = jobInfo.minSalary
        if(!ObjectUtil.hasNull(minSalary, subscription.minSalary)) {
            if(minSalary!! < subscription.minSalary!!) {
                return false
            }
        }
        val minExperience = jobInfo.minExperience
        if(!ObjectUtil.hasNull(minExperience, subscription.maxExperience)) {
            if(minExperience!! > subscription.maxExperience!!) {
                return false
            }
        }
        return !hasBlockWords(jobInfo, subscription)
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
    
    fun hasKeyword(jobInfo: JobInfo, keyword: String): Boolean {
        val realKeyword = keyword.lowercase()
        val propertiesToCheck = jobInfo.run {
            listOf(title, tags, details)
        }
        val result = propertiesToCheck.firstOrNull {
            it?.lowercase()?.contains(realKeyword) == true
        }
        return result != null
    }
    
    fun getCommutingDuration(jobInfo: JobInfo, subscription: Subscription): Int? = run {
        runCatching {
            doGetCommutingDuration(jobInfo, subscription)
        }.getOrElse {
            exceptionReportService.report(it)
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
                        setHttpProxy("localhost", port)
                    }
                    execute().body().toJsonWrapper()
                }
                if(res.getStr("status") != "1") return null
                val minDuration = res.getArray("route.transits").minOfOrNull {
                    it.cast<JSONObject>().getStr("duration").toInt()
                }
                return minDuration?.let { it / 60 }
            }
            else -> exception("Not support the platform: ${jobInfo.platform}")
        }
    }
}
