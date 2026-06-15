package io.kudos.ability.cache.interservice.client.init

import feign.Request
import feign.RequestTemplate
import feign.Response
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [JacksonDecoder].
 *
 * Builds Feign [Response] objects directly (no Spring, no HTTP) and asserts:
 *  - empty-response shortcuts: 204 / 404 / missing body all decode to null;
 *  - String target type reads the raw body as UTF-8 (including non-ASCII characters);
 *  - any other target type goes through Jackson deserialization.
 *
 * @author K
 * @since 1.0.0
 */
internal class JacksonDecoderTest {

    private val decoder = JacksonDecoder(ObjectMapper())

    @Test
    fun decode_status204_returnsNull() {
        assertNull(decoder.decode(newResponse(204, "ignored"), String::class.java))
    }

    @Test
    fun decode_status404_returnsNull() {
        assertNull(decoder.decode(newResponse(404, "ignored"), String::class.java))
    }

    @Test
    fun decode_missingBody_returnsNull() {
        assertNull(decoder.decode(newResponse(200, body = null), String::class.java))
    }

    @Test
    fun decode_stringType_readsRawUtf8Body() {
        val text = "héllo 世界 😀"
        val result = decoder.decode(newResponse(200, text), String::class.java)
        assertEquals(text, result)
    }

    @Test
    fun decode_emptyStringBody_returnsEmptyString() {
        val result = decoder.decode(newResponse(200, ""), String::class.java)
        assertEquals("", result)
    }

    @Test
    fun decode_jsonObject_deserializesViaJackson() {
        val result = decoder.decode(newResponse(200, """{"name":"kudos","num":3}"""), Map::class.java)
        assertEquals(mapOf("name" to "kudos", "num" to 3), result)
    }

    private fun newResponse(status: Int, body: String? = null): Response {
        val request = Request.create(
            Request.HttpMethod.GET,
            "http://localhost/x",
            emptyMap(),
            Request.Body.empty(),
            RequestTemplate(),
        )
        val builder = Response.builder()
            .status(status)
            .headers(emptyMap())
            .request(request)
        if (body != null) {
            builder.body(body.toByteArray(StandardCharsets.UTF_8))
        }
        return builder.build()
    }
}
