package io.kudos.base.logger.slf4j

import io.kudos.base.logger.ILog
import io.kudos.base.logger.ILogCreator
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Creator for the SLF4J-backed logger.
 *
 * @author K
 * @since 1.0.0
 */
open class Slf4jLoggerCreator : ILogCreator {

    override fun createLog(clazz: KClass<*>): ILog = Slf4jLogger(LoggerFactory.getLogger(clazz.java))

}