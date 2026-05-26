package io.kudos.ability.web.ktor.core

import io.ktor.server.application.*
import io.kudos.ability.web.ktor.init.KtorProperties

/**
 * Ktor context.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object KtorContext {

    /**
     * Ktor application object.
     */
    lateinit var application: Application

    /**
     * Kudos Ktor configuration.
     */
    lateinit var properties: KtorProperties

    /** For scenarios such as Spring shutdown to decide: test mode or "not started" does not assign [application]. */
    fun isApplicationInitialized(): Boolean = ::application.isInitialized
}