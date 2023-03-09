package io.kudos.base.logger.slf4j

import io.kudos.base.logger.ILogger
import org.soul.base.log.slf4j.Slf4jLog

/**
 * slf4j实现的日志记录器
 *
 * @author K
 * @since 1.0.0
 */
open class Slf4jLogger(logger: org.slf4j.Logger) : Slf4jLog(logger), ILogger {

}