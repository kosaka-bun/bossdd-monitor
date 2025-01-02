package de.honoka.bossddmonitor.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId

data class JobPushRecord(
    
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    
    var jobInfoId: Long? = null,
    
    /**
     * 订阅此岗位的用户ID（默认情况下为QQ号）
     */
    var subscribeUserId: Long? = null,
    
    /**
     * 推送记录创建时的用户住址（经纬度）
     */
    var userGpsLocation: String? = null,
    
    /**
     * 此岗位通勤时间（分钟）
     */
    var commuteDuration: Int? = null,
    
    /**
     * 是否已向用户推送此岗位
     */
    var pushed: Boolean? = null,
    
    /**
     * 该记录是否有效（是否符合用户设定的筛选条件）
     */
    var valid: Boolean? = null
)
