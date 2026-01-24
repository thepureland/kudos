package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 部门ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：auth_dept_user
 * 2.缓存各用户所属的所有部门ID列表
 * 3.缓存的key为：userId
 * 4.缓存的value为：部门ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class DeptIdsByUserIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Autowired
    private lateinit var authUserDao: AuthUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_DEPT_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<DeptIdsByUserIdCacheHandler>().getDeptIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的部门ID！")
            return
        }

        // 加载所有active=true的用户
        val userCriteria = Criteria(AuthUser::active.name, OperatorEnum.EQ, true)
        val users = authUserDao.search(userCriteria)
        
        // 加载所有部门-用户关系
        @Suppress("UNCHECKED_CAST")
        val allDeptUsers = authDeptUserDao.allSearch()
        val userIdToDeptIdsMap = allDeptUsers
            .groupBy { it.userId }
            .mapValues { entry -> entry.value.map { it.deptId } }

        log.debug("从数据库加载了${users.size}条用户、${allDeptUsers.size}条部门-用户关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户部门ID列表
        users.forEach { user ->
            val deptIds = userIdToDeptIdsMap[user.id!!] ?: emptyList()
            if (deptIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, user.id!!, deptIds)
                log.debug("缓存了用户${user.id}的${deptIds.size}条部门ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户所属的所有部门ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param userId 用户ID
     * @return List<部门ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getDeptIds(userId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的部门ID，从数据库中加载...")
        }

        val deptUserCriteria = Criteria(AuthDeptUser::userId.name, OperatorEnum.EQ, userId)
        val deptIds = authDeptUserDao.searchProperty(deptUserCriteria, AuthDeptUser::deptId.name)
        
        log.debug("从数据库加载了用户${userId}的${deptIds.size}条部门ID。")
        @Suppress("UNCHECKED_CAST")
        return deptIds as List<String>
    }

    /**
     * 用户-部门关系变更后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnDeptUserChange(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${userId}的部门关系变更后，同步${CACHE_NAME}缓存...")
            evict(userId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<DeptIdsByUserIdCacheHandler>().getDeptIds(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户-部门关系变更后同步缓存
     *
     * @param userIds 用户ID集合
     */
    open fun syncOnBatchDeptUserChange(userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户部门关系变更后，同步${CACHE_NAME}缓存...")
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<DeptIdsByUserIdCacheHandler>().getDeptIds(userId)
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
