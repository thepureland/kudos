package io.kudos.ms.user.core.login.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import io.kudos.ms.user.common.login.vo.UserLoginRememberMeCacheEntry
import io.kudos.ms.user.core.login.dao.UserLoginRememberMeDao
import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDateTime


/**
 * Remember-me login cache handler
 *
 * 1. Source table: user_login_remember_me
 * 2. Cache key: tenant_id::username
 * 3. Cache value: UserLoginRememberMeCacheEntry object
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class RememberMeByTenantIdAndUsernameCache : AbstractKeyValueCacheHandler<UserLoginRememberMeCacheEntry>() {


    @Autowired
    private lateinit var userLoginRememberMeDao: UserLoginRememberMeDao

    companion object {
        private const val CACHE_NAME = "REMEMBER_ME_BY_TENANT_ID_AND_USERNAME"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): UserLoginRememberMeCacheEntry? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "The key format of cache ${CACHE_NAME} must be: tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}username"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        require(parts.size == 2) {
            "The key format of cache ${CACHE_NAME} must be: tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}username"
        }
        return getSelf<RememberMeByTenantIdAndUsernameCache>().getRememberMe(parts[0], parts[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skip loading and caching all remember-me login records!")
            return
        }

        val rows = userLoginRememberMeDao.allSearchProperties(
            listOf(
                UserLoginRememberMe::tenantId,
                UserLoginRememberMe::username,
                UserLoginRememberMe::token,
                UserLoginRememberMe::lastUsed,
                UserLoginRememberMe::id
            )
        )
        log.debug("Loaded ${rows.size} remember-me login records from the database.")

        if (clear) {
            clear()
        }

        rows.forEach { row ->
            val tenantId = (row[UserLoginRememberMe::tenantId.name] as String?)?.trim() ?: return@forEach
            val username = (row[UserLoginRememberMe::username.name] as String?)?.trim() ?: return@forEach
            val cacheItem = buildCacheItem(row, username)
            KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, username), cacheItem)
        }
        log.debug("Cached ${rows.size} remember-me login records.")
    }

    /**
     * Get the remember-me login record by tenant ID and username from the cache;
     * if missing, load from the database and write back to the cache.
     *
     * @param tenantId tenant ID
     * @param username username
     * @return UserLoginRememberMeCacheEntry, or null if not found
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null"
    )
    open fun getRememberMe(tenantId: String, username: String): UserLoginRememberMeCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No remember-me login record for tenant ${tenantId} and username ${username} in cache; loading from the database...")
        }
        val trimmedTenantId = tenantId.trim()
        val trimmedUsername = username.trim()
        val criteria = Criteria.of(UserLoginRememberMe::tenantId.name, OperatorEnum.EQ, trimmedTenantId)
            .addAnd(UserLoginRememberMe::username.name, OperatorEnum.EQ, trimmedUsername)
        val rows = userLoginRememberMeDao.searchProperties(
            criteria,
            listOf(
                UserLoginRememberMe::id,
                UserLoginRememberMe::username,
                UserLoginRememberMe::token,
                UserLoginRememberMe::lastUsed
            )
        )
        return if (rows.isEmpty()) {
            log.warn("No remember-me login record for tenant ${trimmedTenantId} and username ${trimmedUsername} in the database!")
            null
        } else {
            buildCacheItem(rows.first(), trimmedUsername)
        }
    }

    /**
     * Sync the cache after inserting a database record
     *
     * @param any object containing the required properties
     * @param id remember-me login id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Remember-me login record with id ${id} inserted; syncing ${CACHE_NAME} cache...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RememberMeByTenantIdAndUsernameCache>().getRememberMe(tenantId, username)
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync the cache after updating a database record
     *
     * @param any object containing the required properties
     * @param id remember-me login id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Remember-me login record with id ${id} updated; syncing ${CACHE_NAME} cache...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RememberMeByTenantIdAndUsernameCache>().getRememberMe(tenantId, username)
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync the cache after deleting a database record
     *
     * @param any object containing the required properties
     * @param id remember-me login id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Remember-me login record with id ${id} deleted; evicting from ${CACHE_NAME} cache...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync the cache after batch deleting database records
     *
     * @param ids remember-me login id collection
     * @param tenantIdAndUsernames List<Pair<tenantId, username>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, tenantIdAndUsernames: List<Pair<String, String>>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Remember-me login records with ids ${ids} batch-deleted; evicting from ${CACHE_NAME} cache...")
            tenantIdAndUsernames.forEach {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(it.first, it.second))
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Return the key built from the parameters
     *
     * @param tenantId tenant ID
     * @param username username
     * @return cache key
     */
    fun getKey(tenantId: String, username: String): String {
        return "${tenantId.trim()}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username.trim()}"
    }

    private fun buildCacheItem(row: Map<String, *>, username: String): UserLoginRememberMeCacheEntry {
        return UserLoginRememberMeCacheEntry(
            id = (row[UserLoginRememberMe::id.name] as String?) ?: "",
            username = username,
            token = row[UserLoginRememberMe::token.name] as String?,
            lastUsed = row[UserLoginRememberMe::lastUsed.name] as LocalDateTime?
        )
    }

    /**
     * Resolve the `(tenantId, username)` pair used to rebuild the cache key.
     *
     * Two-path strategy:
     * 1. Prefer reflective reads from `any` (usually an event payload / PO) — no DB round trip, fastest
     * 2. If fields are missing, look up the DB by id; if still not found, log WARN and return null
     *    (callers should skip sync on null)
     *
     * Trim strings after reflection: the DB may contain dirty data with trailing spaces; avoid
     * leaking dirty data into the cache key segment.
     *
     * @param any event payload or PO
     * @param id  record id (used as a DB fallback)
     * @return `(tenantId, username)`; null if the record does not exist
     * @author K
     * @since 1.0.0
     */
    private fun resolveKeyParts(any: Any, id: String): Pair<String, String>? {
        val tenantId = (BeanKit.getProperty(any, UserLoginRememberMe::tenantId.name) as String?)?.trim()
        val username = (BeanKit.getProperty(any, UserLoginRememberMe::username.name) as String?)?.trim()
        if (tenantId != null && username != null) {
            return tenantId to username
        }
        val rows = userLoginRememberMeDao.oneSearchProperties(
            UserLoginRememberMe::id,
            id,
            listOf(UserLoginRememberMe::tenantId, UserLoginRememberMe::username)
        )
        if (rows.isEmpty()) {
            log.warn("Record with id ${id} not found while syncing the remember-me login cache.")
            return null
        }
        val row = rows.first()
        val dbTenantId = (row[UserLoginRememberMe::tenantId.name] as String?)?.trim()
        val dbUsername = (row[UserLoginRememberMe::username.name] as String?)?.trim()
        if (dbTenantId.isNullOrBlank() || dbUsername.isNullOrBlank()) {
            log.warn("Unable to obtain tenantId or username for id ${id} while syncing the remember-me login cache.")
            return null
        }
        return dbTenantId to dbUsername
    }

    private val log = LogFactory.getLog(this::class)


}
