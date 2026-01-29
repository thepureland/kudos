package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.provider.dao.AuthRoleUserDao
import io.kudos.ams.auth.provider.model.po.AuthRoleUser
import io.kudos.ams.user.provider.dao.AuthUserDao
import io.kudos.ams.user.provider.model.po.AuthUser
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 角色ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：auth_role_user
 * 2.缓存各用户拥有的所有角色ID列表
 * 3.缓存的key为：userId
 * 4.缓存的value为：角色ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class RoleIdsByUserIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authUserDao: AuthUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_ROLE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<RoleIdsByUserIdCacheHandler>().getRoleIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的角色ID！")
            return
        }

        // 加载所有active=true的用户
        val userCriteria = Criteria(AuthUser::active.name, OperatorEnum.EQ, true)
        val users = authUserDao.search(userCriteria)
        
        // 加载所有角色-用户关系
        val allRoleUsers = authRoleUserDao.allSearch()
        val userIdToRoleIdsMap = allRoleUsers
            .groupBy { it.userId }
            .mapValues { entry -> entry.value.map { it.roleId } }

        log.debug("从数据库加载了${users.size}条用户、${allRoleUsers.size}条角色-用户关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户角色ID列表
        users.forEach { user ->
            val roleIds = userIdToRoleIdsMap[user.id!!] ?: emptyList()
            if (roleIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, user.id!!, roleIds)
                log.debug("缓存了用户${user.id}的${roleIds.size}条角色ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户拥有的所有角色ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param userId 用户ID
     * @return List<角色ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getRoleIds(userId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的角色ID，从数据库中加载...")
        }

        val roleUserCriteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        val roleIds = authRoleUserDao.searchProperty(roleUserCriteria, AuthRoleUser::roleId.name)
        
        log.debug("从数据库加载了用户${userId}的${roleIds.size}条角色ID。")
        @Suppress("UNCHECKED_CAST")
        return roleIds as List<String>
    }

    /**
     * 用户-角色关系变更后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnRoleUserChange(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${userId}的角色关系变更后，同步${CACHE_NAME}缓存...")
            evict(userId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RoleIdsByUserIdCacheHandler>().getRoleIds(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户-角色关系变更后同步缓存
     *
     * @param userIds 用户ID集合
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户角色关系变更后，同步${CACHE_NAME}缓存...")
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<RoleIdsByUserIdCacheHandler>().getRoleIds(userId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
        }
    }

    /**
     * 用户删除后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnUserDelete(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除用户${userId}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, userId)
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
