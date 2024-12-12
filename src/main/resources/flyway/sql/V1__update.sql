drop table if exists job_info;
create table job_info
(
    id                bigint auto_increment primary key,
    platform_job_id   varchar(255) comment '平台岗位ID',
    identifiers       text comment '岗位标识符（json）',
    city_code         varchar(255) comment '城市代码',
    title             varchar(255) comment '岗位标题',
    company           varchar(255) comment '公司名（简称）',
    company_full_name varchar(255) comment '公司名',
    company_scale     varchar(255) comment '公司规模',
    hr_name           varchar(255) comment 'HR姓名',
    hr_activeness     varchar(255) comment 'HR活跃度',
    salary            varchar(255) comment '薪资范围',
    experience        varchar(255) comment '经验要求',
    edu_degree        varchar(255) comment '学历要求',
    tags              text comment '岗位标签（json）',
    details           text comment '岗位详细描述',
    address           varchar(255) comment '岗位地址',
    gps_location      varchar(255) comment '岗位地址（经纬度）'
) comment '岗位信息表';

drop table if exists job_push_record;
create table job_push_record
(
    id                bigint auto_increment primary key,
    job_info_id       bigint,
    subscribe_user_id bigint comment '订阅此岗位的用户ID（默认情况下为QQ号）',
    user_gps_location varchar(255) comment '用户住址（经纬度）',
    commute_duration  int comment '此岗位通勤时间（分钟）',
    pushed            tinyint comment '是否已向用户推送此岗位'
) comment '岗位推送记录表';
