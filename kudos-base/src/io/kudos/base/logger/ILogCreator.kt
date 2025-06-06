package io.kudos.base.logger

import kotlin.reflect.KClass

/**
 * 日志记录器创建者
 *
 * @author K
 * @since 1.0.0
 */
interface ILogCreator {

    /**
     * 创建日志记录器
     *
     * @param clazz 日志记录器所处的类
     * @return 日志记录器
     * @author K
     * @since 1.0.0
     */
    fun createLog(clazz: KClass<*>): ILog

}