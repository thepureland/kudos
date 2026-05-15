package io.kudos.context.retry

import java.io.File

/**
 * 失败数据重试框架的路径配置。
 *
 * **重构动因**：原 [IFailedDataHandler.filePath] 与下游 `StreamProducerExceptionHandler.filePath()`
 * 各自硬编码 `/var/data/failed`——
 * - 在 Windows 上路径无效
 * - 在没有挂载 volume 的容器里不可写
 * - 两处独立硬编码，修改时容易漏改一处
 *
 * 现统一从此对象解析。优先级：
 * 1. 系统属性 `kudos.retry.failed-data-path`
 * 2. 环境变量 `KUDOS_RETRY_FAILED_DATA_PATH`
 * 3. 默认 `${java.io.tmpdir}/kudos-failed-data`（跨平台安全）
 *
 * 解析结果只读，应用启动后不会变（`by lazy`，首次访问后冻结）。
 *
 * @author K
 * @since 1.0.0
 */
object RetryConfig {

    /** 系统属性 key */
    const val SYS_PROP_BASE_PATH = "kudos.retry.failed-data-path"

    /** 环境变量 key */
    const val ENV_VAR_BASE_PATH = "KUDOS_RETRY_FAILED_DATA_PATH"

    private const val DEFAULT_DIR_NAME = "kudos-failed-data"

    /**
     * 失败数据持久化的根目录。
     *
     * 注意此处用 `by lazy`：首次访问时按上述优先级解析并缓存，**后续修改系统属性
     * 或环境变量不会生效**。这是有意的——确保整个 JVM 生命周期内路径一致。
     * 测试需要切换路径时请走 [resolveBasePath] 直接调用。
     */
    val baseFailedDataPath: String by lazy { resolveBasePath() }

    /**
     * 按当前优先级解析根目录（用于测试或显式刷新场景）。
     */
    internal fun resolveBasePath(): String {
        System.getProperty(SYS_PROP_BASE_PATH)?.takeIf { it.isNotBlank() }?.let { return it }
        System.getenv(ENV_VAR_BASE_PATH)?.takeIf { it.isNotBlank() }?.let { return it }
        val tmp = System.getProperty("java.io.tmpdir").trimEnd('/', '\\')
        return tmp + File.separator + DEFAULT_DIR_NAME
    }

    /**
     * 为指定原子服务构建持久化根目录。
     *
     * @param atomicServiceCode 原子服务编码，可为 null（如非 HTTP 请求线程上下文缺失）。
     *                          null / 空白时用 `"default"` 作占位子目录，避免拼出 `.../null` 之类的脏路径。
     */
    fun pathFor(atomicServiceCode: String?): String {
        val service = atomicServiceCode?.takeIf { it.isNotBlank() } ?: "default"
        return baseFailedDataPath + File.separator + service
    }
}
