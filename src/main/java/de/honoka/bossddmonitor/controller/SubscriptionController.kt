package de.honoka.bossddmonitor.controller

import de.honoka.bossddmonitor.service.SubscriptionService
import de.honoka.qqrobot.framework.api.model.RobotMessage
import de.honoka.qqrobot.starter.command.CommandMethodArgs
import de.honoka.qqrobot.starter.common.annotation.Command
import de.honoka.qqrobot.starter.common.annotation.RobotController
import de.honoka.qqrobot.starter.component.session.SessionManager

@RobotController
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val sessionManager: SessionManager
) {
    
    @Command("我的订阅")
    fun getSubscription(args: CommandMethodArgs): String = run {
        subscriptionService.getSubscriptionOfUser(args.qq)
    }
    
    @Command("注册")
    fun register(args: CommandMethodArgs) {
        sessionManager.openSession(args.group, args.qq) {
            action = {
                subscriptionService.create(this)
            }
        }
    }
    
    @Command("修改订阅", argsCount = 2)
    fun updateSubscription(args: CommandMethodArgs): String = run {
        subscriptionService.update(args)
    }
    
    @Command("查询屏蔽词", argsCount = 1)
    fun getBlockWordsAndRegexes(args: CommandMethodArgs): RobotMessage<*> = run {
        subscriptionService.getBlockWordsAndRegexes(args.qq, args.getString(0))
    }
    
    @Command("管理屏蔽词", argsCount = 3)
    fun manageBlockWordsAndRegexes(args: CommandMethodArgs): String = run {
        subscriptionService.manageBlockWordsAndRegexes(args)
    }
}
