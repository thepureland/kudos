package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import java.io.Serial
import java.io.Serializable

/**
 * 缓存配置信息
 *
 * **字段语义**：
 * - [strategyDictCode]：来源于 DB 字典码（如 sys_cache.strategyDictCode）。
 * - [strategy]：来源于代码 / yml（如 `kudos.cache.items` 解析后）。
 *
 * 这两个字段在历史上分别由"DB 持久化"和"代码配置"两条路径写入，调用方处处写
 * `config.strategy ?: config.strategyDictCode`，容易遗漏。统一使用 [resolvedStrategy]
 * 这个派生属性来读，避免各处重复兜底。两个原始字段保留，确保 DB 反序列化与 yml 绑定都不破。
 *
 * @author K
 * @since 1.0.0
 */
class CacheConfig : Serializable {

    /**
     * 名称
     */
    var name: String? = ""

    /**
     * 缓存策略代码（来自 DB 字典码）。建议读侧用 [resolvedStrategy]；只在写入 DB / 反序列化场景才直接读这个字段。
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

    /**
     * 缓存策略代码（来自代码 / yml 配置）。@Transient 表示不入 DB。建议读侧用 [resolvedStrategy]。
     */
    @Transient
    var strategy: String? = null

    @Transient
    var ignoreVersion: Boolean? = null

    /**
     * 是否为 Hash 缓存（带 id 对象集合）。true 时参与 MixHashCacheManager 初始化，策略仍用 [resolvedStrategy]。
     */
    var hash: Boolean = false

    /**
     * 派生的策略字符串：优先用代码 / yml 来源的 [strategy]，回退到 DB 字典码 [strategyDictCode]。
     * 旧代码大量写 `config.strategy ?: config.strategyDictCode`，本属性把这套兜底集中到一处。
     */
    val resolvedStrategyCode: String?
        get() = strategy ?: strategyDictCode

    /**
     * 派生的强类型策略。把 [resolvedStrategyCode] 解析为 [CacheStrategy]；解析失败或缺失返回 null。
     * 调用方通常应当用这个，而不是再去自行 [CacheStrategy.valueOf]。
     */
    val resolvedStrategy: CacheStrategy?
        get() = resolvedStrategyCode?.let {
            try {
                CacheStrategy.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    /** 是否启用：null 视为 true（旧行为：初始默认是 true，但反序列化场景可能拿到 null）。 */
    val isActive: Boolean get() = active != false

    /** 是否启动时回写：null 视为 false。集中到一处避免各处 `== true` 风格散落。 */
    val isWriteOnBoot: Boolean get() = writeOnBoot == true

    /** 是否及时回写：null 视为 false。 */
    val isWriteInTime: Boolean get() = writeInTime == true

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
