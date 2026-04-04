package io.kudos.ability.web.ktor.core

import io.ktor.server.application.*
import io.kudos.ability.web.ktor.init.KtorProperties

/**
 * Ktor上下文
 *
 * @author K
 * @since 1.0.0
 */
object KtorContext {

    /**
     * Ktor的application对象
     */
    lateinit var application: Application

    /**
     * kudos的Ktor配置
     */
    lateinit var properties: KtorProperties

    /** 供 Spring 关闭等场景判断：test 模式或未启动时不会赋值 [application] */
    fun isApplicationInitialized(): Boolean = ::application.isInitialized
}