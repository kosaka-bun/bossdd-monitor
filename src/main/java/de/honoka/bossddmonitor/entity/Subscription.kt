package de.honoka.bossddmonitor.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId

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
     * 岗位的最大年限要求
     */
    var maxSeniorityYears: Int? = null,
    
    /**
     * 岗位最低薪资待遇（单位：千）
     */
    var minSalary: Int? = null,
    
    /**
     * 用户住址（经纬度）
     */
    var userGpsLocation: String? = null
)
