package de.honoka.bossddmonitor.platform

import cn.hutool.json.JSONObject
import de.honoka.bossddmonitor.common.ExtendedExceptionReporter
import de.honoka.bossddmonitor.common.ServiceLauncher
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.service.BrowserService
import de.honoka.bossddmonitor.service.JobInfoService
import de.honoka.bossddmonitor.service.JobPushRecordService
import de.honoka.sdk.util.kotlin.text.*
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.RejectedExecutionException

@Component
class BossddPlatform(
    private val browserService: BrowserService,
    private val jobInfoService: JobInfoService,
    private val jobPushRecordService: JobPushRecordService,
    private val exceptionReporter: ExtendedExceptionReporter
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
        
        private val experienceToParamMap = mapOf(
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
        
        val cityCodeMap = HashMap<String, String>().also {
            @Suppress("JAVA_CLASS_ON_COMPANION")
            val data = javaClass.getResource("/static-data/bossdd/city-code.json")
            data!!.readText().toJsonArray().forEach { jo ->
                jo as JSONObject
                it[jo["code"]!!.toString()] = jo.getStr("name")
                jo.getJSONArray("subLevelModelList")?.forEach { jo2 ->
                    jo2 as JSONObject
                    it[jo2["code"]!!.toString()] = jo2.getStr("name")
                }
            }
            HashMap(it).forEach { (k, v) ->
                it[v] = k
            }
        }
    }
    
    override fun doDataExtracting(subscription: Subscription) {
        val urlPrefix = """
            https://www.zhipin.com/web/geek/job?query=${subscription.searchWord}&
            city=${subscription.cityCode}&scale=${getScaleParamValue(subscription)}&
            experience=${getExperienceParamValue(subscription)}&jobType=1901&
            salary=${getSalaryParamValue(subscription)}
        """.singleLine()
        fun url(page: Int) = "$urlPrefix&page=$page"
        val apiUrl = "https://www.zhipin.com/wapi/zpgeek/search/joblist.json"
        repeat(10) { i ->
            val res = browserService.waitForResponse(url(i + 1), apiUrl) {
                it.toJsonWrapper().getInt("code") == 0
            }
            res.toJsonWrapper().getArray("zpData.jobList").forEachWrapper {
                if(ServiceLauncher.appShutdown) return
                try {
                    val platformJobId = it.getStr("encryptJobId")
                    val existingJobInfo = jobInfoService.baseMapper.findByPlatformJobId(
                        PlatformEnum.BOSSDD, platformJobId
                    )
                    if(jobInfoService.shouldUpdateIncrement(existingJobInfo)) {
                        val incrementJobInfo = parseIncrementJobInfo(it)
                        incrementJobInfo.id = existingJobInfo!!.id
                        jobInfoService.updateById(incrementJobInfo)
                        return@forEachWrapper
                    }
                    val jobInfo = parseJobInfo(it).apply {
                        fromSearchWord = subscription.searchWord
                    }
                    existingJobInfo ?: run {
                        runCatching {
                            jobInfoService.isEligible(jobInfo, subscription)
                        }.getOrDefault(true).let { b ->
                            if(!b) return@forEachWrapper
                        }
                    }
                    jobInfo.parseJobInfoDetails(it)
                    if(existingJobInfo == null) {
                        jobInfoService.save(jobInfo)
                    } else {
                        jobPushRecordService.baseMapper.removeInvalidRecords(existingJobInfo.id!!)
                        jobInfo.id = existingJobInfo.id
                        jobInfoService.updateById(jobInfo)
                    }
                    jobPushRecordService.checkAndCreate(jobInfo)
                } catch(t: Throwable) {
                    if(t is RejectedExecutionException) return@forEachWrapper
                    exceptionReporter.report(t)
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
    
    private fun JobInfo.parseJobInfoDetails(jsonWrapper: JsonWrapper) {
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
    
    private fun getScaleParamValue(subscription: Subscription): String {
        val minScale = subscription.minCompanyScale!!
        val params = minScaleToParamMap.filter { (k) -> k >= minScale }.values
        return params.joinToString(",")
    }
    
    private fun getExperienceParamValue(subscription: Subscription): String {
        val maxYears = subscription.maxExperience!!
        val params = experienceToParamMap.filter { (k) -> k <= maxYears }.values
        return params.joinToString(",")
    }
    
    private fun getSalaryParamValue(subscription: Subscription): String {
        val minSalary = subscription.minSalary!!
        val param = salaryToParamMap.entries.run {
            if(minSalary <= 10) {
                firstOrNull { it.key >= minSalary }
            } else {
                lastOrNull { minSalary >= it.key }
            }
        }
        return param?.value ?: salaryToParamMap.entries.last().value
    }
}
