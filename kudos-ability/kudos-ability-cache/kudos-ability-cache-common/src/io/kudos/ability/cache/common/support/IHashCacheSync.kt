package io.kudos.ability.cache.common.support

/**
 * Hash-cache "local-only cleanup" extension interface, used to evict local storage upon receiving a Redis notification.
 * Not exposed to business write logic.
 *
 * Only local implementations (e.g., CaffeineIdEntitiesHashCache) need to implement this; remote implementations do not.
 *
 * @author K
 * @since 1.0.0
 */
interface IHashCacheSync {

    /**
     * Clears the local primary data and indexes under the given cacheName (corresponds to remote refreshAll).
     */
    fun clearLocal(cacheName: String)

    /**
     * Removes the given id from local storage only (corresponds to remote deleteById, or to invalidating the id after save).
     */
    fun evictLocal(cacheName: String, id: Any)
}
