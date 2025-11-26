package io.kudos.ability.web.ktor.plugins

import io.ktor.server.testing.*
import io.kudos.context.core.currentKudosContext
import kotlin.test.Test
import kotlin.test.assertFalse

class KudosContextPluginTest {

    @Test
    fun test() = testApplication {
        //TODO
//        assertFalse(currentKudosContext().traceKey.isNullOrEmpty())
    }

}