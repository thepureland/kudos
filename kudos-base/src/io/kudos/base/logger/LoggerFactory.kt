package io.kudos.base.logger

import io.kudos.base.logger.slf4j.Slf4jLoggerCreator
import kotlin.reflect.KClass

/**
 * 日志记录器工厂，用于创建日志记录器
 *
 * @author K
 * @since 1.0.0
 */
object LoggerFactory {

    private val loggerCreator: LoggerCreator = Slf4jLoggerCreator()

    /**
     * 获取日志记录器
     *
     * @param clazz 使用该日志记录器的类
     * @return 指定类的日志记录器
     * @author K
     * @since 1.0.0
     */
    fun getLogger(clazz: KClass<*>): ILogger = loggerCreator.createLog(clazz)

    /**
     * 获取日志记录器
     *
     * @param any 使用该日志记录器的类对象
     * @return 指定类对象的日志记录器
     * @author K
     * @since 1.0.0
     */
    fun getLogger(any: Any): ILogger = loggerCreator.createLog(any::class)

}