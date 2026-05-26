package io.kudos.base.logger

import io.kudos.base.logger.slf4j.Slf4jLoggerCreator
import kotlin.reflect.KClass

/**
 * Logger factory used to create loggers.
 *
 * @author K
 * @since 1.0.0
 */
object LogFactory {

    private val logCreator: ILogCreator = Slf4jLoggerCreator()

    /**
     * Returns a logger.
     *
     * @param clazz the class that uses the logger
     * @return the logger for the given class
     * @author K
     * @since 1.0.0
     */
    fun getLog(clazz: KClass<*>): ILog = logCreator.createLog(clazz)

    /**
     * Returns a logger.
     *
     * @param any the instance that uses the logger
     * @return the logger for the instance's class
     * @author K
     * @since 1.0.0
     */
    fun getLog(any: Any): ILog = logCreator.createLog(any::class)

}