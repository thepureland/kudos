package io.kudos.ability.cache.interservice.common

/**
 * 请求结果载休
 *
 * @author K
 * @since 1.0.0
 */
data class RequestResult(
    var code: Int?,
    var msg: String?
) {

    // 反射调用
    constructor(): this(null, null)

}