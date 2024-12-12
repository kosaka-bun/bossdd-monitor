package de.honoka.bossddmonitor.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.entity.JobInfo
import de.honoka.bossddmonitor.mapper.JobInfoMapper
import org.springframework.stereotype.Service

@Service
class JobInfoService : ServiceImpl<JobInfoMapper, JobInfo>() {

}