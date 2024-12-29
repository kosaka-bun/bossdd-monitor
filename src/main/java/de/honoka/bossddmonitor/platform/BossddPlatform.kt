package de.honoka.bossddmonitor.platform

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
    
    override fun doDataExtracting(subscription: Subscription) {
        fun url(page: Int) = """
            https://www.zhipin.com/web/geek/job?query=${subscription.searchWord}&
            city=${subscription.cityCode}&page=$page
        """.singleLine()
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
        val urlPrefix = "https://www.zhipin.com/job_detail/$platformJobId.html"
        val url = "$urlPrefix?lid=${identifiersMap["lid"]}&securityId=${identifiersMap["securityId"]}"
        val html = browserService.waitForResponse(url, urlPrefix)
        val doc = Jsoup.parse(html)
        jsonWrapper.let {
            companyFullName = doc.expectFirst("li.company-name").run {
                getElementsByTag("span").forEach { it.remove() }
                text().trim()
            }
            hrLiveness = doc.expectFirst("span.boss-active-time").text().trim()
            details = doc.expectFirst("div.job-sec-text").html().process {
                replace(Regex("<br\\s?/?>"), "\n")
                replace(Regex("\n\n\n+"), "\n\n")
                trim()
            }
            address = doc.expectFirst("div.location-address").text().trim()
        }
    }
    
    private fun parseIncrementJobInfo(jsonWrapper: JsonWrapper): JobInfo = JobInfo().apply {
        jsonWrapper.let {
            hrOnline = it.getBool("bossOnline")
            updateTime = Date()
        }
    }
}
