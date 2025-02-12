package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.platform.PlatformEnum
import de.honoka.sdk.spring.starter.mybatis.queryChainWrapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface JobInfoMapper : BaseMapper<JobInfo> {
    
    fun findByPlatformJobId(platform: PlatformEnum, platformJobId: String): JobInfo? {
        queryChainWrapper().run {
            eq(JobInfo::platform, platform)
            eq(JobInfo::platformJobId, platformJobId)
            last("limit 1")
            return one()
        }
    }
    
    fun getNoRecordsJobInfoList(@Param("userId") userId: Long): List<JobInfo>
}
