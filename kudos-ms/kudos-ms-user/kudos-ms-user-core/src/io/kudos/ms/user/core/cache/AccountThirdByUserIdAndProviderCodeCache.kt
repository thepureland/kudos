package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.user.common.vo.user.UserAccountThirdCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountThirdSearchPayload
import io.kudos.ms.user.core.dao.UserAccountThirdDao
import io.kudos.ms.user.core.model.po.UserAccountThird
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户第三方账号缓存处理器
 *
 * 1.数据来源表：user_account_third
 * 2.缓存所有active=true的第三方账号信息
 * 3.缓存的key为：user_id::account_provider_dict_code
 * 4.缓存的value为：UserAccountThirdCacheItem对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
//region your codes 1
open class AccountThirdByUserIdAndProviderCodeCache : AbstractKeyValueCacheHandler<UserAccountThirdCacheItem>() {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userAccountThirdDao: UserAccountThirdDao

    companion object {
        private const val CACHE_NAME = "ACCOUNT_THIRD_BY_USER_ID_AND_PROVIDER_CODE"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): UserAccountThirdCacheItem? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是：userId${Consts.CACHE_KEY_DEFAULT_DELIMITER}accountProviderDictCode"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(
            parts[0], parts[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的第三方账号信息！")
            return
        }

        val searchPayload = UserAccountThirdSearchPayload().apply {
            returnEntityClass = UserAccountThirdCacheItem::class
            active = true
        }
        @Suppress("UNCHECKED_CAST")
        val results = userAccountThirdDao.search(searchPayload) as List<UserAccountThirdCacheItem>
        log.debug("从数据库加载了${results.size}条第三方账号信息。")

        if (clear) {
            clear()
        }

        results.forEach { item ->
            val userId = item.userId ?: return@forEach
            val providerCode = item.accountProviderDictCode ?: return@forEach
            CacheKit.put(CACHE_NAME, getKey(userId, providerCode), item)
        }
        log.debug("缓存了${results.size}条第三方账号信息。")
    }

    /**
     * 根据用户ID和提供方代码获取第三方账号信息，如果缓存中不存在，则从数据库加载并写入缓存
     *
     * @param userId 用户ID
     * @param accountProviderDictCode 第三方平台字典码
     * @return UserAccountThirdCacheItem，找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#accountProviderDictCode)",
        unless = "#result == null"
    )
    open fun getAccountThird(userId: String, accountProviderDictCode: String): UserAccountThirdCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}且提供方为${accountProviderDictCode}的第三方账号，从数据库中加载...")
        }
        val searchPayload = UserAccountThirdSearchPayload().apply {
            returnEntityClass = UserAccountThirdCacheItem::class
            this.userId = userId
            this.accountProviderDictCode = accountProviderDictCode
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val results = userAccountThirdDao.search(searchPayload) as List<UserAccountThirdCacheItem>
        return if (results.isEmpty()) {
            log.warn("数据库中不存在用户${userId}且提供方为${accountProviderDictCode}的active=true的第三方账号！")
            null
        } else {
            results.first()
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 第三方账号id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的第三方账号后，同步${CACHE_NAME}缓存...")
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            CacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(userId, providerCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 第三方账号id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的第三方账号后，同步${CACHE_NAME}缓存...")
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            CacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(userId, providerCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 第三方账号id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的第三方账号的启用状态后，同步缓存...")
            val accountThird = userAccountThirdDao.get(id)
            if (accountThird == null) {
                log.warn("同步第三方账号缓存时未找到id为${id}的记录。")
                return
            }
            val key = getKey(accountThird.userId, accountThird.accountProviderDictCode)
            CacheKit.evict(CACHE_NAME, key)
            if (active && CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccountThirdByUserIdAndProviderCodeCache>().getAccountThird(
                    accountThird.userId, accountThird.accountProviderDictCode
                )
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 第三方账号id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val userId = BeanKit.getProperty(any, UserAccountThird::userId.name) as String
            val providerCode = BeanKit.getProperty(any, UserAccountThird::accountProviderDictCode.name) as String
            log.debug("删除id为${id}的第三方账号后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(userId, providerCode))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 第三方账号id集合
     * @param userIdAndProviderCodes List<Pair<用户ID，提供方字典码>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, userIdAndProviderCodes: List<Pair<String, String>>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的第三方账号后，同步从${CACHE_NAME}缓存中踢除...")
            userIdAndProviderCodes.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.first, it.second))
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param userId 用户ID
     * @param accountProviderDictCode 第三方平台字典码
     * @return 缓存key
     */
    fun getKey(userId: String, accountProviderDictCode: String): String {
        return "${userId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${accountProviderDictCode}"
    }

    private val log = LogFactory.getLog(this)

    //endregion your codes 2

}
