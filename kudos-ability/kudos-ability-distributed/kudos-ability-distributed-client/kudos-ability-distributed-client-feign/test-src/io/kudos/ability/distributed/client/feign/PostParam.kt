package io.kudos.ability.distributed.client.feign

data class PostParam (

    var num: Int?,

    var value: String?

) {
    constructor(): this(null, null)
}