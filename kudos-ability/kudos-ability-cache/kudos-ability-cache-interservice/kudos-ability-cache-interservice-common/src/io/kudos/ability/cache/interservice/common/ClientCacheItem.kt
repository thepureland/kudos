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

    companion object {
        @Serial
        private const val serialVersionUID = 1112894179070136297L

        /**
         * 根据对象生成uuid
         *
         * @param obj
         */
        fun genUid(obj: Any): String {
            val md5 = DigestKit.getMD5(
                (obj::class.java.name + JsonKit.toJson(obj)).toByteArray(),
                "feignCache"
            )
            return requireNotNull(md5) { "feignCache MD5 计算结果为空" }
        }
    }
}
