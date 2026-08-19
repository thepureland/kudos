package io.kudos.ability.cache.interservice.client.http

import java.io.Serial
import java.io.Serializable

/**
 * A cached HTTP response body, kept as raw bytes together with the content type it was served with.
 *
 * **The content type is not optional detail.** A `304` reply carries no body and therefore no
 * `Content-Type`; replaying the cached bytes under the 304's headers hands the message converters an
 * `application/octet-stream` response, and they refuse it with
 * `UnknownContentTypeException: no suitable HttpMessageConverter found`. The type has to travel with
 * the bytes from the `200` that produced them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class CachedResponseBody(
    val contentType: String?,
    val bytes: ByteArray,
) : Serializable {

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }
}
