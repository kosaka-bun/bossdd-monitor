package de.honoka.bossddmonitor.common

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy

object GlobalComponents {
    
    val scheduledExecutor = ScheduledThreadPoolExecutor(2, AbortPolicy()).apply {
        /*
         * 任务取消时将定时任务的待执行单元从队列中删除，默认是false。在默认情况下，如果直接取消任务，
         * 并不会从队列中删除此任务的待执行单元。
         *
         * 譬如，一个定时任务被设置为每5秒触发一次，则该任务在每次开始执行时，都会向队列中添加一个该
         * 任务的待执行单元，并在5秒后自动开始执行。
         */
        removeOnCancelPolicy = true
        executeExistingDelayedTasksAfterShutdownPolicy = false
        continueExistingPeriodicTasksAfterShutdownPolicy = false
    }
}