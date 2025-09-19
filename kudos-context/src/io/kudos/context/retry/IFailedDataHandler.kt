package io.kudos.context.retry

import io.kudos.context.core.KudosContextHolder
import java.io.File

interface IFailedDataHandler<T> {
    /**
     * 定义业务类型，用于文件目录区分
     */
    val businessType: String?

    /**
     * 接收失败数据并持久化到本地
     */
    fun persistFailedData(data: T): String?

    /**
     * 定义重试的 CRON 表达式
     */
    val cronExpression: String?

    /**
     * 定时任务触发时，处理持久化的文件
     */
    fun handleFailedData(file: File): Boolean

    fun filePath(): String {
        return "/var/data/failed/" + KudosContextHolder.get().atomicServiceId
    }
}
