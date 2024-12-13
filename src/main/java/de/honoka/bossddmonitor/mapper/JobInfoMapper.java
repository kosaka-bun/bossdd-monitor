package de.honoka.bossddmonitor.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import de.honoka.bossddmonitor.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobInfoMapper extends BaseMapper<JobInfo> {
    
    default Long findIdByPlatformJobId(JobInfo.PlatformEnum platform, String platformJobId) {
        JobInfo result = selectOne(
            new LambdaQueryWrapper<JobInfo>()
                .select(JobInfo::getId)
                .eq(JobInfo::getPlatform, platform)
                .eq(JobInfo::getPlatformJobId, platformJobId)
                .last("limit 1")
        );
        return result != null ? result.getId() : null;
    }
}
