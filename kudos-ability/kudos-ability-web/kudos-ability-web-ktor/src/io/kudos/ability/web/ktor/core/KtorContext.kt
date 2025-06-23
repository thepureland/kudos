package io.kudos.ability.web.ktor.core

import io.ktor.server.application.Application
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
    lateinit var application : Application

    /**
     * kudos的Ktor配置
     */
    lateinit var properties: KtorProperties

}