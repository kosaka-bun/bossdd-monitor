package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.sdk.spring.starter.mybatis.queryChainWrapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface JobInfoMapper : BaseMapper<JobInfo> {
    
    fun findIdByPlatformJobId(platform: PlatformEnum, platformJobId: String): Long? {
        queryChainWrapper().run {
            select(JobInfo::id)
            eq(JobInfo::platform, platform)
            eq(JobInfo::platformJobId, platformJobId)
            last("limit 1")
            return one()?.id
        }
    }
    
    fun getNoRecordsJobInfoList(@Param("userId") userId: Long): List<JobInfo>
}
