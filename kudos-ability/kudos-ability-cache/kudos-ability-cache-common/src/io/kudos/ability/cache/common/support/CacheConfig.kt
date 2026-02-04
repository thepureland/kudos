package io.kudos.ability.cache.common.support

import java.io.Serial
import java.io.Serializable

class CacheConfig : Serializable {

    /**
     * 名称
     */
    var name: String? = ""

    /**
     * 缓存策略代码
     */
    var strategyDictCode: String? = null

    /**
     * 是否启动时写缓存
     */
    var writeOnBoot: Boolean? = null

    /**
     * 是否及时回写缓存
     */
    var writeInTime: Boolean? = null

    /**
     * 缓存生存时间(秒)
     */
    var ttl: Int? = null

    /**
     * 是否启用
     */
    var active: Boolean? = true

    @Transient
    var strategy: String? = null

    @Transient
    var ignoreVersion: Boolean? = null

    /**
     * 是否为 Hash 缓存（带 id 对象集合）。true 时参与 MixHashCacheManager 初始化，策略仍用 strategyDictCode。
     */
    var hash: Boolean? = null

    constructor()

    constructor(
        name: String?,
        strategyDictCode: String?,
        writeOnBoot: Boolean?,
        writeInTime: Boolean?,
        ttl: Int?,
        active: Boolean?
    ) {
        this.name = name
        this.strategyDictCode = strategyDictCode
        this.writeOnBoot = writeOnBoot
        this.writeInTime = writeInTime
        this.ttl = ttl
        this.active = active
    }

    companion object {
        @Serial
        private val serialVersionUID = -3447772148273247925L
    }

}
