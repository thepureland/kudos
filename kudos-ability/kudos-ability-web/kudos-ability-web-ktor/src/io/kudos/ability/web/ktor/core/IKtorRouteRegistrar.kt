package io.kudos.ability.web.ktor.core

import io.ktor.server.routing.*


/**
 * Ktor route registrar interface.
 *
 * Note: classes implementing this interface must be registered as Spring beans.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IKtorRouteRegistrar {

    /**
     * Registers routes.
     *
     * @param routing routing object
     */
    fun register(routing: Routing)

}