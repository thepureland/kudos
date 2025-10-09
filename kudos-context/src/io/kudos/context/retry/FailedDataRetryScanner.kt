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


    @jakarta.annotation.PostConstruct
    fun scheduleAll() {
        val handlers = SpringKit.getBeansOfType(IFailedDataHandler::class)
        handlers.values.forEach(Consumer { handler: IFailedDataHandler<*>? ->
            taskScheduler.schedule({
                lockRetry(
                    handler, KudosContextHolder.get().atomicServiceCode
                )
            }, CronTrigger(handler!!.cronExpression))
            logger.info("Scheduled retry for ${handler.businessType} [${handler.cronExpression}]")
        })
    }

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

    private fun retry(handler: IFailedDataHandler<*>) {
        val dir = Paths.get(handler.filePath(), handler.businessType)
        if (!Files.exists(dir)) {
            return
        }
        try {
            Files.list(dir)
                .filter { p: Path? ->
                    val name = p!!.fileName.toString()
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
