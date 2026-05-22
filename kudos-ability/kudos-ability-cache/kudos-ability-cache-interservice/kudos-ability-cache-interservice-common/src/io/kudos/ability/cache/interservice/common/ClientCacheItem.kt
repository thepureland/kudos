package io.kudos.ability.cache.interservice.common

import io.kudos.base.data.json.JsonKit
import io.kudos.base.security.DigestKit
import java.io.Serial
import java.io.Serializable

/**
 * 客户端缓存项
 * 封装服务间缓存的缓存项，包括唯一标识和缓存数据
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
     * 转成显式 JSON 快照 envelope，避免跨节点传输时只能依赖 JVM 原生序列化。
     *
     * [cacheData] 的 JSON 反序列化需要调用方按自身 DTO 类型完成，因此这里只记录类型名与 JSON 字符串。
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
         * 根据对象生成 uuid，用作跨服务 Feign 响应的内容指纹（客户端用它判断是否复用本地缓存）。
         *
         * 稳定性约定：
         * - 输入：同一类、同一字段值 → 同一 UID（依赖 kotlinx.serialization 按字段声明顺序输出，已是确定的）。
         * - 类型隔离：FQN 与 JSON 之间用 `#` 分隔，杜绝"类名 + JSON 拼接出同一字符串"的边角碰撞。
         * - 已知风险：DTO 里如果带 `Map<*, *>` 且不是 `LinkedHashMap`，迭代顺序可能不稳定 → JSON 不稳 → UID 抖动。
         *   建议接口层避免直接返回原始 Map；如必须，使用 `LinkedHashMap` 或自行排序后再返回。
         *
         * 与"加密"无关，纯指纹用途。MD5 在此处足够低成本且离散性可接受。
         */
        fun genUid(obj: Any): String {
            val fingerprint = obj::class.java.name + "#" + JsonKit.toJson(obj)
            val md5 = DigestKit.getMD5(fingerprint.toByteArray(), "feignCache")
            return requireNotNull(md5) { "feignCache MD5 计算结果为空" }
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
