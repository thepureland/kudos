package io.kudos.ability.cache.common.support

import org.springframework.beans.factory.InitializingBean

/**
 * Cache clean event listener: implement this interface and register it as a bean; [cleanCache]
 * is invoked whenever a local/remote cache entry is evicted.
 *
 * Typical uses: synchronously notify other cluster nodes to invalidate their local copies (avoiding dirty reads),
 * or write an audit log recording the eviction reason.
 * It extends [InitializingBean] so that `afterPropertiesSet` can register the listener with the global listener list.
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheCleanListener : InitializingBean {
    /**
     * Cache clean callback.
     *
     * @param cacheName name of the cache region that was cleaned
     * @param key the cleaned key; `null` means the whole region was cleared
     * @author K
     * @since 1.0.0
     */
    fun cleanCache(cacheName: String, key: Any?)
}
