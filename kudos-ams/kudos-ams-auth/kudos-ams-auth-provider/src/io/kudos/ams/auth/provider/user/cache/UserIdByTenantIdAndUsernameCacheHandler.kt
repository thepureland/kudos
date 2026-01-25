package io.kudos.ams.auth.provider.user.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.provider.user.dao.AuthUserDao
import io.kudos.ams.auth.provider.user.model.po.AuthUser
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户ID（by tenant id & username）缓存处理器
 *
 * 1.数据来源表：auth_user
 * 2.缓存所有active=true的用户ID
 * 3.缓存的key为：tenantId::username
 * 4.缓存的value为：用户ID（String）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdByTenantIdAndUsernameCacheHandler : AbstractCacheHandler<String>() {

    @Autowired
    private lateinit var authUserDao: AuthUserDao

    companion object Companion {
        private const val CACHE_NAME = "AUTH_USER_ID_BY_TENANT_ID_AND_USERNAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): String? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}用户名"
        }
        val tenantAndUsername = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<UserIdByTenantIdAndUsernameCacheHandler>().getUserId(
            tenantAndUsername[0], tenantAndUsername[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的用户ID！")
            return
        }

        // 加载所有可用的用户（注：reloadAll批量加载时直接查DB效率更高）
        val userCriteria = Criteria(AuthUser::active.name, OperatorEnum.EQ, true)
        val users = authUserDao.search(userCriteria)
        log.debug("从数据库加载了${users.size}条用户信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户ID
        users.forEach {
            CacheKit.put(CACHE_NAME, getKey(it.tenantId, it.username), it.id!!)
        }
        log.debug("缓存了${users.size}条用户ID信息。")
    }

    /**
     * 根据租户ID和用户名从缓存获取对应的用户ID，如果缓存中不存在，则从数据库中加载，并写回缓存
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户ID，找不到返回null
     */
    @Cacheable(
        value = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null"
    )
    open fun getUserId(tenantId: String, username: String): String? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}且用户名为${username}的用户ID，从数据库中加载...")
        }

        val userCriteria = Criteria().apply {
            addAnd(AuthUser::tenantId.name, OperatorEnum.EQ, tenantId)
            addAnd(AuthUser::username.name, OperatorEnum.EQ, username)
            addAnd(AuthUser::active.name, OperatorEnum.EQ, true)
        }

        val users = authUserDao.search(userCriteria)
        return if (users.isEmpty()) {
            log.debug("从数据库找不到租户${tenantId}且用户名为${username}的active=true的用户。")
            null
        } else {
            log.debug("从数据库加载了租户${tenantId}且用户名为${username}的用户ID。")
            users.first().id
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 用户id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的用户后，同步${CACHE_NAME}缓存...")
            val tenantId = BeanKit.getProperty(any, AuthUser::tenantId.name) as String
            val username = BeanKit.getProperty(any, AuthUser::username.name) as String
            CacheKit.put(CACHE_NAME, getKey(tenantId, username), id) // 直接缓存ID
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 用户id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的用户后，同步${CACHE_NAME}缓存...")
            val user = if (any == null) {
                authUserDao.get(id)!!
            } else {
                val tenantId = BeanKit.getProperty(any, AuthUser::tenantId.name) as String
                val username = BeanKit.getProperty(any, AuthUser::username.name) as String
                AuthUser().apply {
                    this.id = id
                    this.tenantId = tenantId
                    this.username = username
                }
            }
            CacheKit.evict(CACHE_NAME, getKey(user.tenantId, user.username)) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdByTenantIdAndUsernameCacheHandler>().getUserId(user.tenantId, user.username) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 用户id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的用户的启用状态后，同步缓存...")
            val authUser = authUserDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    CacheKit.put(CACHE_NAME, getKey(authUser.tenantId, authUser.username), id)
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(authUser.tenantId, authUser.username)) // 踢除缓存
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 用户id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val tenantId = BeanKit.getProperty(any, AuthUser::tenantId.name) as String
            val username = BeanKit.getProperty(any, AuthUser::username.name) as String
            log.debug("删除id为${id}的用户后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, username)) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 用户id集合
     * @param tenantAndUsernames List<Pair<租户ID，用户名>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, tenantAndUsernames: List<Pair<String, String>>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的用户后，同步从${CACHE_NAME}缓存中踢除...")
            tenantAndUsernames.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.first, it.second)) // 踢除缓存
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
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username}"
    }

    private val log = LogFactory.getLog(this)

}
