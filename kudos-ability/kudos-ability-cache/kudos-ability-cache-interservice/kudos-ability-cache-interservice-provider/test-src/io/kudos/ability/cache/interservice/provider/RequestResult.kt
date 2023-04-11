package io.kudos.ability.cache.interservice.provider

class RequestResult(
    var code: Int?,
    var msg: String?
) {

    constructor(): this(null, null)

}