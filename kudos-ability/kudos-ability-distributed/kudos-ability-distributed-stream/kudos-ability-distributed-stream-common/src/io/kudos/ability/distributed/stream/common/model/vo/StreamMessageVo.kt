package io.kudos.ability.distributed.stream.common.model.vo

import java.io.Serializable

/**
 * Message object.
 *
 * @param <T>
</T> */
class StreamMessageVo<T> : Serializable {
    /**
     * Message body data.
     */
    var data: T? = null

    constructor()

    constructor(data: T?) {
        this.data = data
    }

    companion object {
        private const val serialVersionUID = 7741695533775765117L
    }
}