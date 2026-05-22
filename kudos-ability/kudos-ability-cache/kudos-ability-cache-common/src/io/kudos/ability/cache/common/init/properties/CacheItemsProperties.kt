package io.kudos.ability.cache.common.init.properties

import io.kudos.ability.cache.common.support.CacheConfig

/**
 * 缓存项配置容器。兼容两种配置形式：
 *
 * 1. `kudos.ability.cache.cache-items[]`：每项是一条字符串形式的查询串。
 * 2. `kudos.ability.cache.cache-item-configs[]`：每项是结构化的 [CacheConfig] 对象。
 *
 * ```yaml
 * kudos:
 *   ability:
 *     cache:
 *       cache-items:
 *         - name=USER_CACHE&strategy=LOCAL_REMOTE&ttl=900
 *         - name=DEMO&strategy=REMOTE&ttl=1800&writeOnBoot=true
 *       cache-item-configs:
 *         - name: ORDER_CACHE
 *           strategy: LOCAL_REMOTE
 *           ttl: 900
 * ```
 *
 * 字符串解析逻辑见 [io.kudos.ability.cache.common.support.DefaultCacheConfigProvider.cacheItemToConfig]：
 * 按 `&` 拆 token，按 `=` 拆 key/value，反射 set 到 `CacheConfig` 字段。
 *
 * **设计权衡**：用扁平字符串而非嵌套 yml object，是为了让长列表在 yml 里更紧凑、好 diff；
 * 代价是失去 IDE 字段提示和编译期校验，所以解析期对未知字段会抛错。新配置建议优先使用
 * [cacheItemConfigs]，保留 [cacheItems] 是为了兼容既有 yml。
 *
 * @property cacheItems 字符串列表，每条形如 `name=X&strategy=Y&...`
 * @property cacheItemConfigs 结构化缓存项列表，每项按 [CacheConfig] 字段绑定
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CacheItemsProperties {
    var cacheItems: MutableList<String> = mutableListOf()
    var cacheItemConfigs: MutableList<CacheConfig> = mutableListOf()
}
