package io.kudos.ability.cache.interservice.common

import io.kudos.base.data.json.JsonKit
import io.kudos.base.security.DigestKit
import java.io.Serial
import java.io.Serializable

/**
 * Client-side cache item.
 * Wraps an inter-service cache item, including the unique identifier and cached data.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class ClientCacheItem : Serializable {

    var uuid: String? = null
    var cacheData: Any? = null

    constructor()

    constructor(uid: String, cacheData: Any) {
        this.uuid = uid
        this.cacheData = cacheData
    }

    /**
     * Converts to an explicit JSON snapshot envelope to avoid relying solely on JVM native serialization
     * when transferring across nodes.
     *
     * JSON deserialization of [cacheData] must be performed by the caller against their own DTO type;
     * this snapshot therefore records only the type name and the JSON string.
     */
    fun toSnapshot(): ClientCacheItemSnapshot {
        val data = cacheData
        return ClientCacheItemSnapshot(
            uuid = uuid,
            cacheDataType = data?.javaClass?.name,
            cacheDataJson = data?.let { JsonKit.toJson(it, preserveNull = true) }
        )
    }

    fun toJsonSnapshot(): String = JsonKit.toJson(toSnapshot(), preserveNull = true)

    companion object {
        @Serial
        private const val serialVersionUID = 1112894179070136297L

        /**
         * Generates a uuid from an object, used as the content fingerprint of an inter-service Feign response
         * (the client uses it to decide whether to reuse its local cache).
         *
         * Stability contract:
         * - Input: same class, same field values → same UID (relies on kotlinx.serialization emitting fields in
         *   declaration order, which is deterministic).
         * - Type isolation: FQN and JSON are separated by `#`, eliminating edge collisions where "class name + JSON"
         *   would concatenate into the same string.
         * - Known risk: if the DTO contains a `Map<*, *>` that is not a `LinkedHashMap`, the iteration order may be
         *   unstable → JSON unstable → UID jitter. Interface layers should avoid returning a raw Map; if necessary,
         *   use `LinkedHashMap` or sort before returning.
         *
         * Not for encryption — fingerprinting only. MD5 is low-cost and provides acceptable dispersion here.
         */
        fun genUid(obj: Any): String {
            val fingerprint = obj::class.java.name + "#" + JsonKit.toJson(obj)
            val md5 = DigestKit.getMD5(fingerprint.toByteArray(), "feignCache")
            return requireNotNull(md5) { "feignCache MD5 result is empty" }
        }

        fun fromSnapshot(
            snapshot: ClientCacheItemSnapshot,
            decodeCacheData: (cacheDataType: String?, cacheDataJson: String?) -> Any?
        ): ClientCacheItem = ClientCacheItem().apply {
            uuid = snapshot.uuid
            cacheData = decodeCacheData(snapshot.cacheDataType, snapshot.cacheDataJson)
        }

        fun fromJsonSnapshot(
            snapshotJson: String,
            decodeCacheData: (cacheDataType: String?, cacheDataJson: String?) -> Any?
        ): ClientCacheItem {
            val snapshot = ClientCacheItemSnapshot(
                uuid = JsonKit.getPropertyValue(snapshotJson, "uuid") as? String,
                cacheDataType = JsonKit.getPropertyValue(snapshotJson, "cacheDataType") as? String,
                cacheDataJson = JsonKit.getPropertyValue(snapshotJson, "cacheDataJson") as? String
            )
            return fromSnapshot(snapshot, decodeCacheData)
        }
    }
}

/**
 * Explicit JSON transport snapshot for an inter-service cache item.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class ClientCacheItemSnapshot(
    val uuid: String?,
    val cacheDataType: String?,
    val cacheDataJson: String?
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID = 6045100851186086646L
    }
}
