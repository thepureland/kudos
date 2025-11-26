package io.kudos.ability.distributed.client.feign

data class PostResult (

    var num: Int?,

    var success: Boolean?,

) {
    constructor(): this(null, false)
}