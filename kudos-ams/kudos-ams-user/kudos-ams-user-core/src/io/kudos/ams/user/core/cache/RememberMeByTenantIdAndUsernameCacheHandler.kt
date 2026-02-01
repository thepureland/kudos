package io.kudos.ams.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.user.common.vo.loginremember.UserLoginRememberMeCacheItem
import io.kudos.ams.user.core.dao.UserLoginRememberMeDao
import io.kudos.ams.user.core.model.po.UserLoginRememberMe
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDateTime


/**
 * 记住我登录缓存处理器
 *
 * 1.数据来源表：user_login_remember_me
 * 2.缓存的key为：tenant_id::username
 * 3.缓存的value为：UserLoginRememberMeCacheItem对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
//region your codes 1
open class RememberMeByTenantIdAndUsernameCacheHandler : AbstractCacheHandler<UserLoginRememberMeCacheItem>() {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userLoginRememberMeDao: UserLoginRememberMeDao

    companion object {
        private const val CACHE_NAME = "REMEMBER_ME_BY_TENANT_ID_AND_USERNAME"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): UserLoginRememberMeCacheItem? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是：tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}username"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        require(parts.size == 2) {
            "缓存${CACHE_NAME}的key格式必须是：tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}username"
        }
        return getSelf<RememberMeByTenantIdAndUsernameCacheHandler>().getRememberMe(parts[0], parts[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有记住我登录信息！")
            return
        }

        val rows = userLoginRememberMeDao.allSearchProperties(
            listOf(
                UserLoginRememberMe::tenantId.name,
                UserLoginRememberMe::username.name,
                UserLoginRememberMe::token.name,
                UserLoginRememberMe::lastUsed.name,
                UserLoginRememberMe::id.name
            )
        )
        log.debug("从数据库加载了${rows.size}条记住我登录信息。")

        if (clear) {
            clear()
        }

        rows.forEach { row ->
            val tenantId = (row[UserLoginRememberMe::tenantId.name] as String?)?.trim() ?: return@forEach
            val username = (row[UserLoginRememberMe::username.name] as String?)?.trim() ?: return@forEach
            val cacheItem = buildCacheItem(row, username)
            CacheKit.put(CACHE_NAME, getKey(tenantId, username), cacheItem)
        }
        log.debug("缓存了${rows.size}条记住我登录信息。")
    }

    /**
     * 根据租户ID和用户名从缓存获取记住我登录信息，如果缓存中不存在，则从数据库加载并写入缓存
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return UserLoginRememberMeCacheItem，找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null"
    )
    open fun getRememberMe(tenantId: String, username: String): UserLoginRememberMeCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}且用户名为${username}的记住我登录信息，从数据库中加载...")
        }
        val trimmedTenantId = tenantId.trim()
        val trimmedUsername = username.trim()
        val rows = userLoginRememberMeDao.andSearchProperties(
            mapOf(
                UserLoginRememberMe::tenantId.name to trimmedTenantId,
                UserLoginRememberMe::username.name to trimmedUsername
            ),
            listOf(
                UserLoginRememberMe::id.name,
                UserLoginRememberMe::username.name,
                UserLoginRememberMe::token.name,
                UserLoginRememberMe::lastUsed.name
            )
        )
        return if (rows.isEmpty()) {
            log.warn("数据库中不存在租户${trimmedTenantId}且用户名为${trimmedUsername}的记住我登录信息！")
            null
        } else {
            buildCacheItem(rows.first(), trimmedUsername)
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 记住我登录id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的记住我登录信息后，同步${CACHE_NAME}缓存...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            CacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RememberMeByTenantIdAndUsernameCacheHandler>().getRememberMe(tenantId, username)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 记住我登录id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的记住我登录信息后，同步${CACHE_NAME}缓存...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            CacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RememberMeByTenantIdAndUsernameCacheHandler>().getRememberMe(tenantId, username)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 记住我登录id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的记住我登录信息后，同步从${CACHE_NAME}缓存中踢除...")
            val (tenantId, username) = resolveKeyParts(any, id) ?: return
            CacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 记住我登录id集合
     * @param tenantIdAndUsernames List<Pair<租户ID，用户名>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, tenantIdAndUsernames: List<Pair<String, String>>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的记住我登录信息后，同步从${CACHE_NAME}缓存中踢除...")
            tenantIdAndUsernames.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.first, it.second))
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 缓存key
     */
    fun getKey(tenantId: String, username: String): String {
        return "${tenantId.trim()}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username.trim()}"
    }

    private fun buildCacheItem(row: Map<String, *>, username: String): UserLoginRememberMeCacheItem {
        return UserLoginRememberMeCacheItem(
            id = row[UserLoginRememberMe::id.name] as String?,
            username = username,
            token = row[UserLoginRememberMe::token.name] as String?,
            lastUsed = row[UserLoginRememberMe::lastUsed.name] as LocalDateTime?
        )
    }

    private fun resolveKeyParts(any: Any, id: String): Pair<String, String>? {
        val tenantId = (BeanKit.getProperty(any, UserLoginRememberMe::tenantId.name) as String?)?.trim()
        val username = (BeanKit.getProperty(any, UserLoginRememberMe::username.name) as String?)?.trim()
        if (tenantId != null && username != null) {
            return tenantId to username
        }
        val rows = userLoginRememberMeDao.oneSearchProperties(
            UserLoginRememberMe::id.name,
            id,
            listOf(UserLoginRememberMe::tenantId.name, UserLoginRememberMe::username.name)
        )
        if (rows.isEmpty()) {
            log.warn("同步记住我登录缓存时未找到id为${id}的记录。")
            return null
        }
        val row = rows.first()
        val dbTenantId = (row[UserLoginRememberMe::tenantId.name] as String?)?.trim()
        val dbUsername = (row[UserLoginRememberMe::username.name] as String?)?.trim()
        if (dbTenantId.isNullOrBlank() || dbUsername.isNullOrBlank()) {
            log.warn("同步记住我登录缓存时无法获取id为${id}的tenantId或username。")
            return null
        }
        return dbTenantId to dbUsername
    }

    private val log = LogFactory.getLog(this)

    //endregion your codes 2

}
