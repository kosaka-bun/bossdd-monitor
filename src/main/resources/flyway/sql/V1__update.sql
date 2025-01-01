drop table if exists subscription;
create table subscription
(
    id                  bigint auto_increment primary key,
    user_id             bigint comment '用户ID（默认情况下为QQ号）',
    receiver_group_id   bigint comment '接收推送消息的群号（若为空则使用私聊进行推送）',
    search_word         varchar(255) comment '搜索关键词',
    city_code           varchar(255) comment '城市代码',
    min_company_scale   int comment '岗位的最小公司规模',
    max_seniority_years int comment '岗位的最大年限要求',
    min_salary          int comment '岗位最低薪资待遇（单位：千）',
    block_words         text comment '岗位信息屏蔽关键词（json）',
    block_regexes       text comment '岗位信息屏蔽正则表达式（json）',
    user_gps_location   varchar(255) comment '用户住址（经纬度）'
) comment '用户订阅配置表';

drop table if exists job_info;
create table job_info
(
    id                bigint auto_increment primary key,
    platform          varchar(255) comment '平台名称',
    platform_job_id   varchar(255) comment '平台岗位ID',
    identifiers       text comment '岗位标识符（json）',
    city_code         varchar(255) comment '城市代码',
    title             varchar(255) comment '岗位标题',
    company           varchar(255) comment '公司名（简称）',
    company_full_name varchar(255) comment '公司名',
    company_scale     varchar(255) comment '公司规模',
    hr_name           varchar(255) comment 'HR姓名',
    hr_online         tinyint comment 'HR是否在线',
    hr_liveness       varchar(255) comment 'HR活跃度',
    salary            varchar(255) comment '薪资范围',
    experience        varchar(255) comment '经验要求',
    edu_degree        varchar(255) comment '学历要求',
    tags              text comment '岗位标签（json）',
    details           text comment '岗位详细描述',
    address           varchar(255) comment '岗位地址',
    gps_location      varchar(255) comment '岗位地址（经纬度）',
    create_time       datetime,
    update_time       datetime
) comment '岗位信息表';
create unique index job_info_index_1 on job_info (platform, platform_job_id);

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
