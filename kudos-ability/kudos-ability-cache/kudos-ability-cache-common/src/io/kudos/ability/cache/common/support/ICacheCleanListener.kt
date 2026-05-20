package io.kudos.ability.cache.common.support

import org.springframework.beans.factory.InitializingBean

/**
 * 缓存清除事件监听器：实现该接口并注册为 bean，每次本地/远程缓存条目失效都会回调 [cleanCache]。
 *
 * 典型用途：同步通知集群其他节点失效本地副本（避免脏读）、或写审计日志记录失效原因。
 * 继承 [InitializingBean] 是为了在 `afterPropertiesSet` 中注册到全局 listener 列表。
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheCleanListener : InitializingBean {
    /**
     * 缓存清除回调。
     *
     * @param cacheName 被清的缓存区名
     * @param key 被清的 key；`null` 表示整区清空
     * @author K
     * @since 1.0.0
     */
    fun cleanCache(cacheName: String, key: Any?)
}
