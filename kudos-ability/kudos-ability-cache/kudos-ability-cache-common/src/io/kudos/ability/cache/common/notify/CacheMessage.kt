package io.kudos.ability.cache.common.notify

import java.io.Serial
import java.io.Serializable

/**
 * 缓存通知消息对象
 *
 * @author K
 * @since 1.0.0
 */
class CacheMessage : Serializable {
    var cacheName: String? = null
    var key: Any? = null
    var nodeId: String? = null

    /**
     * 缓存类型：不设或 "kv" 表示 key-value；"hash" 表示 Hash 缓存（带 id 对象集合）。
     */
    var cacheType: String? = null

    constructor()

    constructor(cacheName: String?, key: Any?) {
        this.cacheName = cacheName
        this.key = key
    }

    companion object {
        @Serial
        private const val serialVersionUID = 3525213225652585377L
    }
}
