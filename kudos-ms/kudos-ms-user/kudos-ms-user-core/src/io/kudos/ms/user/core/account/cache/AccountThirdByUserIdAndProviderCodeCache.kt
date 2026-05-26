package io.kudos.ms.user.core.account.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.user.common.account.vo.UserAccountThirdCacheEntry
import io.kudos.ms.user.common.account.vo.request.UserAccountThirdQuery
import io.kudos.ms.user.core.account.dao.UserAccountThirdDao
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * Third-party account cache handler.
 *
 * 1. Source table: user_account_third
 * 2. Caches all third-party accounts with active=true
 * 3. Cache key: user_id::account_provider_dict_code
 * 4. Cache value: UserAccountThirdCacheEntry instance
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class AccountThirdByUserIdAndProviderCodeCache : AbstractKeyValueCacheHandler<UserAccountThirdCacheEntry>() {


    @Autowired
    private lateinit var userAccountThirdDao: UserAccountThirdDao

    companion object {
        private const val CACHE_NAME = "ACCOUNT_THIRD_BY_USER_ID_AND_PROVIDER_CODE"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): UserAccountThirdCacheEntry? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format must be: userId${Consts.CACHE_KEY_DEFAULT_DELIMITER}accountProviderDictCode"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(
            parts[0], parts[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching active third-party accounts!")
            return
        }

        val searchPayload = UserAccountThirdQuery(active = true)
        @Suppress("UNCHECKED_CAST")
        val results = userAccountThirdDao.search(searchPayload, UserAccountThirdCacheEntry::class)
        log.debug("Loaded ${results.size} third-party account records from the database.")

        if (clear) {
            clear()
        }

        results.forEach { item ->
            val userId = item.userId ?: return@forEach
            val providerCode = item.accountProviderDictCode ?: return@forEach
            KeyValueCacheKit.put(CACHE_NAME, getKey(userId, providerCode), item)
        }
        log.debug("Cached ${results.size} third-party account records.")
    }

    /**
     * Get a third-party account by user id and provider code; on cache miss, load from DB and write back.
     *
     * @param userId user id
     * @param accountProviderDictCode third-party provider dict code
     * @return UserAccountThirdCacheEntry, or null if not found
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#accountProviderDictCode)",
        unless = "#result == null"
    )
    open fun getAccountThird(userId: String, accountProviderDictCode: String): UserAccountThirdCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Third-party account for user=${userId} provider=${accountProviderDictCode} not in cache; loading from DB...")
        }
        val searchPayload = UserAccountThirdQuery(
            userId = userId,
            accountProviderDictCode = accountProviderDictCode,
            active = true
        )
        @Suppress("UNCHECKED_CAST")
        val results = userAccountThirdDao.search(searchPayload, UserAccountThirdCacheEntry::class)
        return if (results.isEmpty()) {
            log.warn("No active=true third-party account found for user=${userId} provider=${accountProviderDictCode}!")
            null
        } else {
            results.first()
        }
    }

    /**
     * Sync the cache after a DB insert.
     *
     * @param any object containing the required properties
     * @param id third-party account id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After inserting third-party account id=${id}, syncing ${CACHE_NAME} cache...")
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(userId, providerCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a DB update.
     *
     * @param any object containing the required properties
     * @param id third-party account id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating third-party account id=${id}, syncing ${CACHE_NAME} cache...")
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(userId, providerCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after the active flag is updated.
     *
     * @param id third-party account id
     * @param active whether the account is active
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating active flag of third-party account id=${id}, syncing cache...")
            val accountThird = userAccountThirdDao.get(id)
            if (accountThird == null) {
                log.warn("No third-party account found with id=${id} while syncing cache.")
                return
            }
            val key = getKey(accountThird.userId, accountThird.accountProviderDictCode)
            KeyValueCacheKit.evict(CACHE_NAME, key)
            if (active && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(
                    accountThird.userId, accountThird.accountProviderDictCode
                )
            }
            log.debug("Cache sync complete.")
        }
    }

    /**
     * Sync the cache after a DB delete.
     *
     * @param any object containing the required properties
     * @param id third-party account id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            log.debug("After deleting third-party account id=${id}, evicting from ${CACHE_NAME} cache...")
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a batch DB delete.
     *
     * @param ids third-party account id collection
     * @param userIdAndProviderCodes List<Pair<userId, providerDictCode>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, userIdAndProviderCodes: List<Pair<String, String>>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch deleting third-party accounts ids=${ids}, evicting from ${CACHE_NAME} cache...")
            userIdAndProviderCodes.forEach {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(it.first, it.second))
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Build the cache key by joining the given parameters.
     *
     * @param userId user id
     * @param accountProviderDictCode third-party provider dict code
     * @return cache key
     */
    fun getKey(userId: String, accountProviderDictCode: String): String {
        return "${userId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${accountProviderDictCode}"
    }

    private val log = LogFactory.getLog(this::class)


}
