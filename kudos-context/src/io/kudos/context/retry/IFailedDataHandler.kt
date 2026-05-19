package io.kudos.context.retry

import io.kudos.context.core.KudosContextHolder
import java.io.File

/**
 * 失败数据重试处理器协议。
 *
 * 用于把 MQ 投递、RPC 调用等可能失败的副作用落盘成文件，由 [FailedDataRetryScanner]
 * 按 [cronExpression] 周期扫描并交还本处理器再次执行。本接口只规定"业务类型 + 持久化 + 重试"
 * 三个抽象点，文件路径与原子操作交给 [AbstractFailedDataHandler] / [RetryConfig] 兜底。
 *
 * @param T 失败数据的载体类型
 * @author K
 * @since 1.0.0
 */
interface IFailedDataHandler<T> {
    /**
     * 定义业务类型，用于文件目录区分（同一进程内多种失败数据互不干扰）
     */
    val businessType: String

    /**
     * 定义重试的 CRON 表达式
     */
    val cronExpression: String

    /**
     * 把失败数据持久化到本地，返回写入的文件路径；持久化失败返回 null。
     *
     * @param data 待保存的失败数据
     * @return 落盘后的文件路径；持久化失败返回 null
     * @author K
     * @since 1.0.0
     */
    fun persistFailedData(data: T): String?

    /**
     * 定时任务触发时，处理单个持久化文件。
     * 实现需保证重试成功后清理文件、失败时保留供下轮再试。
     *
     * @param file 持久化下来的失败数据文件
     * @return true 表示已成功处理并清理；false 表示仍需后续重试
     * @author K
     * @since 1.0.0
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
