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
         * Generates a uuid from an object, used as the content fingerprint of an inter-service response
         * (the client uses it to decide whether to reuse its local cache).
         *
         * Stability contract:
         * - Input: same class, same field values → same UID.
         * - Type isolation: FQN and JSON are separated by `#`, eliminating edge collisions where "class name + JSON"
         *   would concatenate into the same string.
         * - Known risk: if the DTO contains a `Map<*, *>` that is not a `LinkedHashMap`, the iteration order may be
         *   unstable → JSON unstable → UID jitter. Interface layers should avoid returning a raw Map; if necessary,
         *   use `LinkedHashMap` or sort before returning.
         *
         * **A content-free fingerprint is rejected rather than used.** `JsonKit.toJson` is
         * `inline fun <reified T>`, and at this call site `T` is inferred as `Any`, for which kotlinx.serialization
         * has no serializer — so serialization actually runs through the reflective fallback. A type that
         * serializes to nothing would hand *every instance* the same fingerprint, and the provider would then
         * answer 304 forever while clients kept serving stale data with nothing logged. Failing the fingerprint
         * instead makes the caller skip negotiation — the response is returned in full, uncached, which is merely
         * slower.
         *
         * **Residual risk (verified, not defended against).** The reflective fallback maps a *throwing* accessor
         * to null rather than propagating, so a DTO whose accessors throw (uninitialised `lateinit`, a lazy proxy
         * outside its session) still serializes to a well-formed object — just with null fields — and slips past
         * the check below, collapsing every instance of that type onto one UID. Rejecting "all fields null" would
         * also reject legitimately empty DTOs, so the guard stops at genuinely content-free output; the real
         * mitigation is not to return such DTOs from a `@ClientCacheable` endpoint. Pinned by
         * `ClientCacheItemTest.genUid_accessorFailuresCollapseToOneUidPerType_documentedLimitation`.
         *
         * Not for encryption — fingerprinting only. MD5 is low-cost and provides acceptable dispersion here.
         *
         * @throws IllegalStateException when the object yields no usable content to fingerprint
         */
        fun genUid(obj: Any): String {
            val json = JsonKit.toJson(obj)
            check(hasContent(json)) {
                "Refusing to fingerprint ${obj::class.java.name}: it serialized to '$json', which carries no field " +
                    "data, so every instance of this type would share one UID and the client would be told its " +
                    "stale copy is current. Check for accessors that throw (uninitialised lateinit, lazy proxies)."
            }
            val fingerprint = obj::class.java.name + "#" + json
            val md5 = DigestKit.getMD5(fingerprint.toByteArray(), "rpcCache")
            return requireNotNull(md5) { "rpcCache MD5 result is empty" }
        }

        /**
         * Whether [json] carries any field data — i.e. it is not blank, `null`, or an empty object/array.
         *
         * An empty collection response is a legitimate value, but it is also indistinguishable from a
         * serialization that silently produced nothing, so both are treated as unusable for fingerprinting.
         * The cost of that conservatism is one uncached response.
         */
        private fun hasContent(json: String?): Boolean {
            val trimmed = json?.trim() ?: return false
            return trimmed.isNotEmpty() && trimmed != "null" && trimmed != "{}" && trimmed != "[]"
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
