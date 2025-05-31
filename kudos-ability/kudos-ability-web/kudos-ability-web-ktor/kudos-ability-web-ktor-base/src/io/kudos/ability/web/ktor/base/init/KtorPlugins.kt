package io.kudos.ability.web.ktor.base.init

import io.ktor.server.application.Application

fun Application.installPlugins() {
    KtorContext.application = this
    //TODO plugins
}