package io.kudos.context.retry

import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.LockTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
@Component
class FailedDataRetryScanner {

    @Autowired
    @Qualifier("failDataTaskScheduler")
    private lateinit var taskScheduler: TaskScheduler


    /**
     * 调度所有失败数据重试任务
     *
     * 在应用启动后，为所有IFailedDataHandler实现类创建定时重试任务。
     *
     * 工作流程：
     * 1. 获取所有IFailedDataHandler实现类的Bean
     * 2. 为每个处理器创建定时任务：
     *    - 使用处理器配置的cronExpression作为执行频率
     *    - 任务执行时调用lockRetry方法（带分布式锁保护）
     * 3. 记录调度信息日志
     *
     * 定时任务：
     * - 每个处理器都有独立的定时任务
     * - 使用CronTrigger支持灵活的调度配置
     * - 任务执行时会获取分布式锁，确保多实例环境下只有一个实例执行
     *
     * 注意事项：
     * - 在@PostConstruct阶段执行，确保所有Bean都已初始化
     * - 每个处理器的重试频率可以独立配置
     * - 使用分布式锁避免多实例重复执行
     */
    @jakarta.annotation.PostConstruct
    fun scheduleAll() {
        val handlers = SpringKit.getBeansOfType<IFailedDataHandler<*>>()
        handlers.values.forEach { handler ->
            taskScheduler.schedule(
                { lockRetry(handler, KudosContextHolder.get().atomicServiceCode) },
                CronTrigger(handler.cronExpression)
            )
            logger.info("Scheduled retry for ${handler.businessType} [${handler.cronExpression}]")
        }
    }

    /**
     * 加锁重试：使用分布式锁保护重试操作
     *
     * 在分布式环境下，确保只有一个实例执行重试操作，避免重复处理。
     *
     * 锁键格式：
     * - `"$FAILED_DATA_RETRY_LOCK_PREFIX{businessType}_{appName}"`
     * - 历史版本曾误写为 `faile-data-retry-`，已统一为 `failed-data-retry-`；多实例部署须同版本前缀以免无法互斥。
     *
     * @param handler 失败数据处理器
     * @param appName 应用名称，用于生成锁键
     */
    private fun lockRetry(handler: IFailedDataHandler<*>, appName: String?) {
        val lockProvider = LockTool.lockProvider
        val key = "$FAILED_DATA_RETRY_LOCK_PREFIX${handler.businessType}_$appName"
        val lock = lockProvider.tryLock(key, 600)
        if (!lock) {
            logger.warn("还有其他任务在处理中，未正确释放锁")
            return
        }
        try {
            retry(handler)
        } finally {
            lockProvider.unLock(key)
        }
    }

    /**
     * 执行重试：扫描失败数据文件并处理
     *
     * @param handler 失败数据处理器，负责处理具体的失败数据
     */
    private fun retry(handler: IFailedDataHandler<*>) {
        val dir = Paths.get(handler.filePath(), handler.businessType)
        if (!Files.exists(dir)) {
            return
        }
        try {
            Files.list(dir).use { stream ->
                stream
                    .filter { p: Path ->
                        val name = p.fileName.toString()
                        Files.isRegularFile(p) && FAILED_DATA_FILE_PATTERN.matches(name)
                    }
                    .sorted()
                    .forEach { path: Path ->
                        val file = path.toFile()
                        var success = false
                        try {
                            success = handler.handleFailedData(file)
                        } catch (e: Exception) {
                            logger.error(e, "Error handling file $path")
                        }
                        if (success) {
                            try {
                                Files.delete(path)
                                logger.info("Deleted file $path")
                            } catch (ioe: IOException) {
                                logger.error(ioe, "Failed to delete file $path")
                            }
                        }
                    }
            }
            val noRegularFilesLeft = Files.list(dir).use { s ->
                !s.anyMatch { path: Path -> Files.isRegularFile(path) }
            }
            if (noRegularFilesLeft) {
                Files.delete(dir)
                logger.info("Deleted empty directory $dir")
            }
        } catch (e: IOException) {
            logger.error(e, "Scanning directory $dir error")
        }
    }

    private val logger = LogFactory.getLog(this::class)

    companion object {
        /** 失败数据重试锁键前缀（已修正历史拼写 faile-data-retry） */
        private const val FAILED_DATA_RETRY_LOCK_PREFIX = "failed-data-retry-"

        private val FAILED_DATA_FILE_PATTERN = Regex("""\d+-[0-9a-fA-F\-]+\.json""")
    }
}
