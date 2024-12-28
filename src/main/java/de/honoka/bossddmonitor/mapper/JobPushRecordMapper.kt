package de.honoka.bossddmonitor.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import de.honoka.bossddmonitor.entity.JobPushRecord
import org.apache.ibatis.annotations.Mapper

@Mapper
interface JobPushRecordMapper : BaseMapper<JobPushRecord>
