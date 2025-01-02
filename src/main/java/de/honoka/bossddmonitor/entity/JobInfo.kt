package de.honoka.bossddmonitor.entity

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONObject
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import de.honoka.bossddmonitor.common.GlobalComponents
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.sdk.util.kotlin.basic.cast
import de.honoka.sdk.util.kotlin.basic.exception
import de.honoka.sdk.util.kotlin.net.http.browserApiHeaders
import de.honoka.sdk.util.kotlin.text.findOne
import de.honoka.sdk.util.kotlin.text.singleLine
import de.honoka.sdk.util.kotlin.text.toJsonArray
import de.honoka.sdk.util.kotlin.text.toJsonWrapper
import java.util.*

data class JobInfo(
    
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    
    /**
     * 平台名称
     */
    var platform: PlatformEnum? = null,
    
    /**
     * 平台岗位ID
     */
    var platformJobId: String? = null,
    
    /**
     * 岗位标识符（json）
     */
    var identifiers: String? = null,
    
    /**
     * 城市代码
     */
    var cityCode: String? = null,
    
    /**
     * 岗位标题
     */
    var title: String? = null,
    
    /**
     * 公司名（简称）
     */
    var company: String? = null,
    
    /**
     * 公司名
     */
    var companyFullName: String? = null,
    
    /**
     * 公司规模
     */
    var companyScale: String? = null,
    
    /**
     * HR姓名
     */
    var hrName: String? = null,
    
    /**
     * HR是否在线
     */
    var hrOnline: Boolean? = null,
    
    /**
     * HR活跃度
     */
    var hrLiveness: String? = null,
    
    /**
     * 薪资范围
     */
    var salary: String? = null,
    
    /**
     * 经验要求
     */
    var experience: String? = null,
    
    /**
     * 学历要求
     */
    var eduDegree: String? = null,
    
    /**
     * 岗位标签（json）
     */
    var tags: String? = null,
    
    /**
     * 岗位详细描述
     */
    var details: String? = null,
    
    /**
     * 岗位地址
     */
    var address: String? = null,
    
    /**
     * 岗位地址（经纬度）
     */
    var gpsLocation: String? = null,
    
    var createTime: Date? = null,
    
    var updateTime: Date? = null
) {
    
    val minSalary: Int?
        get() = salary?.let {
            val range = it.findOne("\\d+-\\d+") ?: return null
            val min = range.split("-").firstOrNull()?.toInt() ?: return null
            if(it.contains("K")) {
                min
            } else if(it.contains("元")) {
                min / 1000
            } else {
                null
            }
        }
    
    fun hasBlockWords(subscription: Subscription): Boolean = subscription.run {
        val propertiesToCheck = listOf(title, company, companyFullName, tags, details, address)
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
    
    fun getCommutingDuration(subscription: Subscription): Int? {
        subscription.maxCommutingDuration ?: return null
        when(platform) {
            PlatformEnum.BOSSDD -> {
                val url = """
                    https://amap-proxy.zpurl.cn/_AMapService/v3/direction/transit/integrated?
                    platform=JS&s=rsv3&logversion=2.0&key=6104503ca2f1d66e900a7e7064c5d880&
                    sdkversion=2.0.6.1&city=%E5%8C%97%E4%BA%AC%E5%B8%82&strategy=&nightflag=0&
                    appname=https%253A%252F%252Fwww.zhipin.com%252Fweb%252Fgeek%252Fmap%252Fpath&
                    origin=${subscription.userGpsLocation}&destination=$gpsLocation&extensions=&
                    s=rsv3&cityd=NaN
                """.singleLine()
                val res = HttpUtil.createGet(url).run {
                    browserApiHeaders()
                    GlobalComponents.proxyForwarder.forwarder?.run {
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
            else -> exception("Not support the platform: $platform")
        }
    }
}
