package io.kudos.ability.web.ktor.base.spring

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.kudos.test.common.init.EnableKudosTest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableKudosTest
class KtorSpringTest {

    @Test
    fun testRoot() = runBlocking {
        val client = HttpClient()
        val response = client.get("/test")
        assertEquals("Hello World!", response.bodyAsText())
    }

}