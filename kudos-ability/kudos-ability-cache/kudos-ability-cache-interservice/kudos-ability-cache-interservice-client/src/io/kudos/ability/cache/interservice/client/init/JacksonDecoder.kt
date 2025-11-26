package io.kudos.ability.cache.interservice.client.init

import feign.Response
import feign.codec.Decoder
import tools.jackson.databind.ObjectMapper
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

class JacksonDecoder(
    private val objectMapper: ObjectMapper
) : Decoder {

    override fun decode(response: Response, type: Type): Any? {
        // 常见的空响应直接返回 null
        if (response.status() == 204 || response.status() == 404 || response.body() == null) {
            return null
        }

        val body = response.body() ?: return null

        body.asInputStream().use { input ->
            // String 特判一下，直接按 UTF-8 读
            if (type == String::class.java) {
                return input.readAllBytes().toString(StandardCharsets.UTF_8)
            }

            val javaType = objectMapper.typeFactory.constructType(type)
            return objectMapper.readValue(input, javaType)
        }
    }
}