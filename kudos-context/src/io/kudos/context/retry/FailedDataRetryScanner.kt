package io.kudos.context.retry

import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.LockTool
import kotlinx.io.IOException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer


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
        handlers.values.forEach(Consumer { handler: IFailedDataHandler<*>? ->
            taskScheduler.schedule({
                lockRetry(
                    handler, KudosContextHolder.get().atomicServiceCode
                )
            }, CronTrigger(requireNotNull(handler) { "handler is null" }.cronExpression))
            logger.info("Scheduled retry for ${handler.businessType} [${handler.cronExpression}]")
        })
    }

    /**
     * 加锁重试：使用分布式锁保护重试操作
     * 
     * 在分布式环境下，确保只有一个实例执行重试操作，避免重复处理。
     * 
     * 工作流程：
     * 1. 生成锁键：基于业务类型和应用名称生成唯一的锁键
     * 2. 尝试获取锁：使用分布式锁尝试获取锁，等待时间600秒
     * 3. 锁获取失败：如果获取锁失败，说明其他实例正在处理，直接返回
     * 4. 锁获取成功：执行重试操作，并在finally块中释放锁
     * 
     * 锁键格式：
     * - "faile-data-retry-{businessType}_{appName}"
     * - 例如："faile-data-retry-mq-producer-fail_service1"
     * 
     * 锁超时：
     * - 等待时间600秒，如果600秒内无法获取锁，说明可能有任务未正确释放锁
     * - 会记录警告日志，但不执行重试操作
     * 
     * 注意事项：
     * - 使用try-finally确保锁一定会被释放
     * - 如果获取锁失败，不会执行重试操作，避免重复处理
     * - 锁的释放在finally块中执行，确保即使重试过程抛出异常也能释放锁
     * 
     * @param handler 失败数据处理器
     * @param appName 应用名称，用于生成锁键
     */
    private fun lockRetry(handler: IFailedDataHandler<*>, appName: String?) {
        val lockProvider = LockTool.lockProvider
        val key = "faile-data-retry-" + handler.businessType + "_" + appName
        val lock: Boolean = lockProvider.tryLock(key, 600)
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
     * 扫描指定目录下的失败数据文件，逐个处理，处理成功后删除文件。
     * 
     * 工作流程：
     * 1. 构建目录路径：{filePath}/{businessType}
     * 2. 检查目录是否存在，不存在则直接返回
     * 3. 扫描目录下的文件：
     *    - 过滤规则：必须是普通文件且文件名匹配"时间戳-UUID.json"格式
     *    - 按文件名排序（时间戳排序，确保按时间顺序处理）
     * 4. 处理每个文件：
     *    - 调用handler.handleFailedData处理文件
     *    - 如果处理成功，删除文件
     *    - 如果处理失败或抛出异常，保留文件等待下次重试
     * 5. 清理空目录：如果目录下没有文件了，删除空目录
     * 
     * 文件命名规则：
     * - 格式：{时间戳}-{UUID}.json
     * - 例如：1704067200000-550e8400-e29b-41d4-a716-446655440000.json
     * - 正则表达式：\\d+-[0-9a-fA-F\\-]+\\.json
     * 
     * 处理策略：
     * - 按文件名排序，确保按时间顺序处理（先处理较早的文件）
     * - 处理成功才删除文件，失败则保留等待下次重试
     * - 单个文件处理失败不影响其他文件的处理
     * 
     * 异常处理：
     * - 文件处理异常：记录错误日志，但不删除文件，等待下次重试
     * - 文件删除异常：记录错误日志，但不影响其他文件的处理
     * - 目录扫描异常：记录错误日志，中断本次扫描
     * 
     * 注意事项：
     * - 文件必须匹配命名规则才会被处理
     * - 处理成功的文件会被立即删除，释放磁盘空间
     * - 空目录会被自动清理，保持文件系统整洁
     * 
     * @param handler 失败数据处理器，负责处理具体的失败数据
     */
    private fun retry(handler: IFailedDataHandler<*>) {
        val dir = Paths.get(handler.filePath(), handler.businessType)
        if (!Files.exists(dir)) {
            return
        }
        try {
            Files.list(dir)
                .filter { p: Path? ->
                    val name = requireNotNull(p) { "path is null" }.fileName.toString()
                    Files.isRegularFile(p)
                            && name.matches("\\d+-[0-9a-fA-F\\-]+\\.json".toRegex())
                }
                .sorted()
                .forEach { path: Path ->
                    val file = path.toFile()
                    var success = false
                    try {
                        success = handler.handleFailedData(file)
                    } catch (e: java.lang.Exception) {
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
            if (Files.list(dir)
                    .noneMatch { path: Path -> Files.isRegularFile(path) }
            ) {
                Files.delete(dir)
                logger.info("Deleted empty directory $dir")
            }
        } catch (e: IOException) {
            logger.error(e, "Scanning directory $dir error")
        }
    }

    private val logger = LogFactory.getLog(this)

}
