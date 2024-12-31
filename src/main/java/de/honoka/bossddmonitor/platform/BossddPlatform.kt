package de.honoka.bossddmonitor.platform

import cn.hutool.core.util.ObjectUtil
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.service.BrowserService
import de.honoka.bossddmonitor.service.JobInfoService
import de.honoka.sdk.util.kotlin.text.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.util.*

@Component
class BossddPlatform(
    private val browserService: BrowserService,
    private val jobInfoService: JobInfoService
) : Platform {
    
    companion object {
        
        private val minScaleToParamMap = mapOf(
            0 to "301",
            20 to "302",
            100 to "303",
            500 to "304",
            1000 to "305",
            10000 to "306"
        )
        
        private val seniorityYearsToParamMap = mapOf(
            0 to "101,103",
            1 to "104",
            3 to "105",
            5 to "106",
            10 to "107"
        )
        
        private val salaryToParamMap = mapOf(
            0 to "402",
            3 to "403",
            5 to "404",
            10 to "405",
            20 to "406",
            50 to "407"
        )
    }
    
    override fun doDataExtracting(subscription: Subscription) {
        val urlPrefix = """
            https://www.zhipin.com/web/geek/job?query=${subscription.searchWord}&
            city=${subscription.cityCode}&scale=${getScaleParamValue(subscription)}&
            experience=${getSeniorityYearsParamValue(subscription)}&jobType=1901&
            salary=${getSalaryParamValue(subscription)}
        """.singleLine()
        fun url(page: Int) = "$urlPrefix&page=$page"
        val apiUrl = "https://www.zhipin.com/wapi/zpgeek/search/joblist.json"
        repeat(10) {
            browserService.ensureIsActive()
            val res = browserService.waitForResponse(url(it + 1), apiUrl) { r ->
                r.toJsonWrapper().getInt("code") == 0
            }
            res.toJsonWrapper().getArray("zpData.jobList").forEachWrapper {
                runCatching {
                    val platform = PlatformEnum.BOSSDD
                    val platformJobId = it.getStr("encryptJobId")
                    jobInfoService.baseMapper.findIdByPlatformJobId(platform, platformJobId)?.run {
                        val incrementJobInfo = parseIncrementJobInfo(it)
                        incrementJobInfo.id = this
                        jobInfoService.updateById(incrementJobInfo)
                        return@forEachWrapper
                    }
                    val jobInfo = parseJobInfo(it)
                    runCatching {
                        if(!isJobInfoValid(jobInfo, subscription)) {
                            return@forEachWrapper
                        }
                    }
                    parseJobInfoDetails(jobInfo, it)
                    jobInfoService.save(jobInfo)
                }
            }
        }
    }
    
    private fun parseJobInfo(jsonWrapper: JsonWrapper): JobInfo = JobInfo().apply {
        val identifiersMap = mapOf(
            "lid" to jsonWrapper.getStr("lid"),
            "securityId" to jsonWrapper.getStr("securityId")
        )
        jsonWrapper.let {
            platform = PlatformEnum.BOSSDD
            platformJobId = it.getStr("encryptJobId")
            identifiers = identifiersMap.toJsonString()
            cityCode = it.getLong("city").toString()
            title = it.getStr("jobName")
            company = it.getStr("brandName")
            companyScale = it.getStr("brandScaleName")
            hrName = it.getStr("bossName")
            hrOnline = it.getBool("bossOnline")
            salary = it.getStr("salaryDesc")
            experience = it.getStr("jobExperience")
            eduDegree = it.getStr("jobDegree")
            tags = it.getArray("skills").toString()
            gpsLocation = "${it.getStr("gps.longitude")},${it.getStr("gps.latitude")}"
            createTime = Date()
            updateTime = createTime
        }
    }
    
    private fun parseJobInfoDetails(jobInfo: JobInfo, jsonWrapper: JsonWrapper) = jobInfo.run {
        val identifiersMap = identifiers!!.toJsonObject()
        val urlPrefix = "https://www.zhipin.com/job_detail/$platformJobId.html"
        val url = "$urlPrefix?lid=${identifiersMap["lid"]}&securityId=${identifiersMap["securityId"]}"
        val html = browserService.waitForResponse(url, urlPrefix)
        val doc = Jsoup.parse(html)
        jsonWrapper.let {
            companyFullName = doc.selectFirst("li.company-name")?.run {
                getElementsByTag("span").forEach { it.remove() }
                text().trim()
            }
            hrLiveness = run {
                if(doc.selectFirst("span.boss-online-tag") != null) {
                    "在线"
                } else {
                    doc.selectFirst("span.boss-active-time")?.run {
                        text().trim()
                    }
                }
            }
            details = doc.selectFirst("div.job-sec-text")?.html()?.process {
                replace(Regex("\\s*<br\\s?/?>\\s*"), "\n")
                replace(Regex("\n\n\n+"), "\n\n")
                trim()
            }
            address = doc.selectFirst("div.location-address")?.run {
                text().trim()
            }
        }
    }
    
    private fun parseIncrementJobInfo(jsonWrapper: JsonWrapper): JobInfo = JobInfo().apply {
        jsonWrapper.let {
            hrOnline = it.getBool("bossOnline")
            updateTime = Date()
        }
    }
    
    private fun isJobInfoValid(jobInfo: JobInfo, subscription: Subscription): Boolean {
        val minSalary = jobInfo.minSalary
        if(!ObjectUtil.hasNull(minSalary, subscription.minSalary)) {
            if(minSalary!! < subscription.minSalary!!) {
                return false
            }
        }
        
        return true
    }
    
    private fun getScaleParamValue(subscription: Subscription): String {
        val minScale = subscription.minCompanyScale!!
        val params = minScaleToParamMap.filter { (k) -> k >= minScale }.values
        return params.joinToString(",")
    }
    
    private fun getSeniorityYearsParamValue(subscription: Subscription): String {
        val maxYears = subscription.maxSeniorityYears!!
        val params = seniorityYearsToParamMap.filter { (k) -> k <= maxYears }.values
        return params.joinToString(",")
    }
    
    private fun getSalaryParamValue(subscription: Subscription): String {
        val minSalary = subscription.minSalary!!
        val param = salaryToParamMap.entries.firstOrNull { (k) -> k >= minSalary }
        return param?.value ?: salaryToParamMap.entries.last().value
    }
}
