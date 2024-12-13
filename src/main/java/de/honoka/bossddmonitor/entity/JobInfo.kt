package de.honoka.bossddmonitor.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId

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
    var gpsLocation: String? = null
) {
    
    enum class PlatformEnum {
        
        BOSSDD
    }
}