package de.honoka.bossddmonitor.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import de.honoka.bossddmonitor.entity.Subscription
import de.honoka.bossddmonitor.mapper.SubscriptionMapper
import de.honoka.bossddmonitor.platform.BossddPlatform
import de.honoka.qqrobot.starter.command.CommandMethodArgs
import de.honoka.qqrobot.starter.component.session.RobotSession
import de.honoka.qqrobot.starter.component.session.SessionManager
import de.honoka.sdk.util.kotlin.text.singleLine
import de.honoka.sdk.util.kotlin.text.trimAllLines
import org.springframework.stereotype.Service

@Service
class SubscriptionService(
    private val sessionManager: SessionManager
) : ServiceImpl<SubscriptionMapper, Subscription>() {
    
    private object ConstMessages {
        
        const val NO_SUBSCRIPTION = "没有找到对应的订阅，请先注册"
    }
    
    fun getSubscriptionOfUser(userId: Long): String {
        val subscription = baseMapper.getByUserId(userId)
        subscription ?: return ConstMessages.NO_SUBSCRIPTION
        val result = subscription.run {
            """
                接收推送消息的群号：$receiverGroupId
                搜索关键词：$searchWord
                城市：${BossddPlatform.cityCodeMap[cityCode]}
                城市代码：$cityCode
                岗位的最小公司规模：$minCompanyScale
                岗位的最大经验要求：${maxExperience}年
                岗位的最低薪资待遇：${minSalary}K
                岗位的最大通勤时间：${maxCommutingDuration}分钟
                用户住址（经纬度）：$userGpsLocation
            """.trimAllLines()
        }
        return result
    }
    
    fun getSubscriptionStatusOfUser(userId: Long): String {
        val subscription = baseMapper.getByUserId(userId)
        subscription ?: return ConstMessages.NO_SUBSCRIPTION
        return if(subscription.enabled!!) "已启用" else "未启用"
    }
    
    fun setSubscriptionStatusOfUser(userId: Long, status: String): String {
        val subscription = baseMapper.getByUserId(userId)
        subscription ?: return ConstMessages.NO_SUBSCRIPTION
        val params = Subscription().apply {
            id = subscription.id
            enabled = when(status) {
                "开" -> true
                "关" -> false
                else -> return "请使用“开”或“关”作为参数值"
            }
            if(enabled == subscription.enabled) {
                return "订阅目前已处于此状态，无需修改"
            }
        }
        updateById(params)
        return getSubscriptionStatusOfUser(userId)
    }
    
    fun create(args: CommandMethodArgs) {
        sessionManager.openSession(args.group, args.qq) {
            action = action@ {
                baseMapper.getByUserId(args.qq)?.let {
                    reply("您已进行过注册，无需重复注册")
                    return@action
                }
                val subscription = parseByRobotSession(this)
                save(subscription)
                """
                    注册成功，订阅状态默认为关闭状态，此时可直接启用订阅，或在配置完成屏蔽词或
                    要屏蔽的正则表达式后再启用
                """.singleLine().let { reply(it) }
            }
        }
    }
    
    private fun parseByRobotSession(session: RobotSession): Subscription = session.run {
        Subscription().apply {
            userId = session.qq
            receiverGroupId = waitForReply(
                "请回复接收推送消息的群号，回复“none”表示使用私聊消息接收",
                "提供的群号有误，请回复数字或“none”",
                {
                    @Suppress("USELESS_IS_CHECK")
                    it.lowercase() == "none" || it.toLong() is Long
                }
            ).let { if(it.lowercase() == "none") null else it.toLong() }
            searchWord = waitForReply(
                "请回复要搜索的关键词",
                resultPredicate = { it.isNotBlank() }
            )
            cityCode = waitForReply(
                "请回复要查找的岗位所在的城市名（地级市或直辖市，不带“市”字）",
                "未找到对应的城市，请重新输入",
                { BossddPlatform.cityCodeMap[it] != null }
            ).let { BossddPlatform.cityCodeMap[it] }
            minCompanyScale = waitForReply(
                "请回复岗位所属公司的最小人数规模（数字）",
                resultPredicate = { it.toInt() >= 0 }
            ).toInt()
            maxExperience = waitForReply(
                "请回复岗位的最大经验要求（年）",
                resultPredicate = { it.toInt() >= 0 }
            ).toInt()
            minSalary = waitForReply(
                "请回复岗位的最低薪资待遇（千，如10K则回复10）",
                resultPredicate = { it.toInt() >= 0 }
            ).toInt()
            maxCommutingDuration = waitForReply(
                "请回复岗位的最大通勤时间（分钟）",
                resultPredicate = { it.toInt() >= 0 }
            ).toInt()
            userGpsLocation = waitForReply(
                "请回复用户住址（经纬度，如“121.320081,31.193964”）",
                resultPredicate = {
                    it.matches(Regex("\\d+\\.\\d+,\\d+\\.\\d+"))
                }
            )
            enabled = false
        }
    }
}
