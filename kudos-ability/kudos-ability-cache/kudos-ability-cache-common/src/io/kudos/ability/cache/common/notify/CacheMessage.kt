package io.kudos.ability.cache.common.notify

import java.io.Serial
import java.io.Serializable

/**
 * Cache notification message object.
 *
 * @author K
 * @since 1.0.0
 */
class CacheMessage : Serializable {
    var cacheName: String? = null
    var key: Any? = null
    var nodeId: String? = null

    /**
     * Cache type: unset or "kv" denotes key-value; "hash" denotes a hash cache (an id-keyed entity collection).
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
