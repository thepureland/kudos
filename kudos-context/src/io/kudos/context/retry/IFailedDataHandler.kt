package io.kudos.context.retry

import io.kudos.context.core.KudosContextHolder
import java.io.File

interface IFailedDataHandler<T> {
    /**
     * 定义业务类型，用于文件目录区分
     */
    val businessType: String

    /**
     * 定义重试的 CRON 表达式
     */
    val cronExpression: String

    /**
     * 接收失败数据并持久化到本地
     */
    fun persistFailedData(data: T): String?

    /**
     * 定时任务触发时，处理持久化的文件
     */
    fun handleFailedData(file: File): Boolean

    /**
     * 失败数据持久化根目录 + 原子服务子目录。
     *
     * 默认走 [RetryConfig.pathFor]：可通过系统属性 `kudos.retry.failed-data-path` 或
     * 环境变量 `KUDOS_RETRY_FAILED_DATA_PATH` 配置，未配置时落到 `${java.io.tmpdir}/kudos-failed-data`，
     * 跨 Windows / Linux / 容器都安全。
     *
     * 子目录走 [KudosContextHolder.getOrNull] 拿原子服务编码——用 `getOrNull` 而非 `get` 是为了
     * 避免非 HTTP 请求线程（如定时任务）触发 [KudosContextHolder] 的隐式创建副作用。
     */
    fun filePath(): String = RetryConfig.pathFor(KudosContextHolder.getOrNull()?.atomicServiceCode)
}
