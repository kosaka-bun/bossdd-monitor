package de.honoka.bossddmonitor.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.entity.JobPushRecord
import de.honoka.bossddmonitor.mapper.JobPushRecordMapper
import org.springframework.stereotype.Service

@Service
class JobPushRecordService : ServiceImpl<JobPushRecordMapper, JobPushRecord>() {

}