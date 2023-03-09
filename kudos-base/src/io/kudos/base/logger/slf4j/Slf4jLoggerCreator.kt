package io.kudos.base.logger.slf4j

import io.kudos.base.logger.ILogger
import io.kudos.base.logger.LoggerCreator
import org.slf4j.LoggerFactory
import org.soul.base.log.LogFactory
import kotlin.reflect.KClass

/**
 * slf4j日志记录器创建者
 *
 * @author K
 * @since 1.0.0
 */
open class Slf4jLoggerCreator : LoggerCreator {

    override fun createLog(clazz: KClass<*>): ILogger = Slf4jLogger(LoggerFactory.getLogger(clazz.java))

}