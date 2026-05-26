package io.kudos.ability.cache.interservice.client.init

import feign.Response
import feign.codec.Decoder
import tools.jackson.databind.ObjectMapper
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

/**
 * Jackson decoder.
 * Used for Feign response decoding; supports deserializing HTTP response bodies into Java objects.
 */
class JacksonDecoder(
    private val objectMapper: ObjectMapper
) : Decoder {

    override fun decode(response: Response, type: Type): Any? {
        // Return null directly for common empty responses
        if (response.status() == 204 || response.status() == 404 || response.body() == null) {
            return null
        }

        val body = response.body() ?: return null

        body.asInputStream().use { input ->
            // Special-case String: read directly as UTF-8
            if (type == String::class.java) {
                return input.readAllBytes().toString(StandardCharsets.UTF_8)
            }

            val javaType = objectMapper.typeFactory.constructType(type)
            return objectMapper.readValue(input, javaType)
        }
    }
}