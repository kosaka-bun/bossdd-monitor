package de.honoka.bossddmonitor.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import org.intellij.lang.annotations.Language

data class Subscription(
    
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    
    /**
     * 用户ID（默认情况下为QQ号）
     */
    var userId: Long? = null,
    
    /**
     * 接收推送消息的群号（若为空则使用私聊进行推送）
     */
    var receiverGroupId: Long? = null,
    
    /**
     * 搜索关键词
     */
    var searchWord: String? = null,
    
    /**
     * 城市代码
     */
    var cityCode: String? = null,
    
    /**
     * 岗位的最小公司规模
     */
    var minCompanyScale: Int? = null,
    
    /**
     * 岗位的最大经验要求（年）
     */
    var maxExperience: Int? = null,
    
    /**
     * 岗位的最低薪资待遇（千）
     */
    var minSalary: Int? = null,
    
    /**
     * 岗位的最大通勤时间（分钟）
     */
    var maxCommutingDuration: Int? = null,
    
    /**
     * 岗位信息屏蔽关键词（json）
     */
    var blockWords: String? = null,
    
    /**
     * 岗位信息屏蔽正则表达式（json）
     */
    var blockRegexes: String? = null,
    
    /**
     * 用户住址（经纬度）
     */
    var userGpsLocation: String? = null,
    
    /**
     * 是否启用此订阅
     */
    var enabled: Boolean? = null
) {
    
    companion object {
        
        @Language("RegExp")
        const val USER_GPS_LOCATION_PATTERN = "\\d+\\.\\d+,\\d+\\.\\d+"
    }
}
