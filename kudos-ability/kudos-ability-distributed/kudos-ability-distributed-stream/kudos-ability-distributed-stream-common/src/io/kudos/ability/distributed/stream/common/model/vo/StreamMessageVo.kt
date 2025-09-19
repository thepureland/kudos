package io.kudos.ability.distributed.stream.common.model.vo

import java.io.Serializable

/**
 * 消息对象
 *
 * @param <T>
</T> */
class StreamMessageVo<T> : Serializable {
    /**
     * 消息体数据
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
