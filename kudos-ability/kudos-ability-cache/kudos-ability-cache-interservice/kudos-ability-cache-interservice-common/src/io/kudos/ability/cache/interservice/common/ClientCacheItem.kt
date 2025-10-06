package io.kudos.ability.cache.interservice.common

import io.kudos.base.data.json.JsonKit
import io.kudos.base.security.DigestKit
import java.io.Serial
import java.io.Serializable

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
            requireNotNull(obj) { "生成uid的对象不能为空！" }
            return DigestKit.getMD5((obj.javaClass.getName() + JsonKit.toJson(obj)).toByteArray(), "feignCache")!!
        }
    }
}
