package io.kudos.ability.distributed.client.http

data class PostParam(

    var num: Int?,

    var value: String?

) {
    constructor() : this(null, null)
}
