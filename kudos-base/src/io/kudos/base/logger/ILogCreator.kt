package io.kudos.base.logger

import kotlin.reflect.KClass

/**
 * Logger creator.
 *
 * @author K
 * @since 1.0.0
 */
interface ILogCreator {

    /**
     * Creates a logger.
     *
     * @param clazz the class the logger belongs to
     * @return the logger
     * @author K
     * @since 1.0.0
     */
    fun createLog(clazz: KClass<*>): ILog

}