package io.kudos.ability.web.ktor.core

import io.ktor.server.routing.*


/**
 * Ktor路由注册器接口
 *
 * 注：实现该接口的类必须注册为Spring的bean
 *
 * @author K
 * @since 1.0.0
 */
interface IKtorRouteRegistrar {

    /**
     * 注册路由
     *
     * @param routing 路由对象
     */
    fun register(routing: Routing)

}