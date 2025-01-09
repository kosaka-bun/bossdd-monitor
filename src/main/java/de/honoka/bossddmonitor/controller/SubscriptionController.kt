package de.honoka.bossddmonitor.controller

import de.honoka.bossddmonitor.service.SubscriptionService
import de.honoka.qqrobot.framework.api.model.RobotMessage
import de.honoka.qqrobot.starter.command.CommandMethodArgs
import de.honoka.qqrobot.starter.common.annotation.Command
import de.honoka.qqrobot.starter.common.annotation.RobotController

@RobotController
class SubscriptionController(private val subscriptionService: SubscriptionService) {
    
    @Command("我的订阅")
    fun getSubscription(args: CommandMethodArgs): String = run {
        subscriptionService.getSubscriptionOfUser(args.qq)
    }
    
    @Command("订阅状态")
    fun getSubscriptionStatus(args: CommandMethodArgs): String = run {
        subscriptionService.getSubscriptionStatusOfUser(args.qq)
    }
    
    @Command("修改订阅状态", argsCount = 1)
    fun setSubscriptionStatus(args: CommandMethodArgs): String = run {
        subscriptionService.setSubscriptionStatusOfUser(args.qq, args.getString(0))
    }
    
    @Command("注册")
    fun register(args: CommandMethodArgs) {
        subscriptionService.create(args)
    }
    
    @Command("查询屏蔽词", argsCount = 1)
    fun getBlockWordsAndRegexes(args: CommandMethodArgs): RobotMessage<*> = run {
        subscriptionService.getBlockWordsAndRegexes(args.qq, args.getString(0))
    }
}
