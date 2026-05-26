package io.kudos.ability.cache.interservice.common

/**
 * Request result payload.
 *
 * @author K
 * @since 1.0.0
 */
data class RequestResult(
    var code: Int?,
    var msg: String?
) {

    // For reflective instantiation
    constructor(): this(null, null)

}