package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户ID列表（by dept）缓存处理器
 *
 * 1.数据来源表：auth_dept_user
 * 2.缓存各部门下的用户ID
 * 3.缓存的key为：deptId
 * 4.缓存的value为：用户ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByDeptIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authDeptUserDao: AuthDeptUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_DEPT_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByDeptIdCacheHandler>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有部门下的用户ID！")
            return
        }

        // 加载所有部门-用户关系，按部门分组
        @Suppress("UNCHECKED_CAST")
        val allDeptUsers = authDeptUserDao.allSearch()
        val deptIdAndUserIdsMap = allDeptUsers
            .groupBy { it.deptId }
            .mapValues { entry -> entry.value.map { it.userId } }

        log.debug("从数据库加载了${allDeptUsers.size}条部门-用户关系信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户ID
        deptIdAndUserIdsMap.forEach { (deptId, userIds) ->
            CacheKit.put(CACHE_NAME, deptId, userIds)
            log.debug("缓存了部门${deptId}的${userIds.size}条用户ID。")
        }
    }

    /**
     * 根据部门ID从缓存中获取其下所有用户ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param deptId 部门ID
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#deptId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(deptId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在部门${deptId}的用户ID，从数据库中加载...")
        }

        val criteria = Criteria(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
        val userIds = authDeptUserDao.searchProperty(criteria, AuthDeptUser::userId.name)
        log.debug("从数据库加载了部门${deptId}的${userIds.size}条用户ID。")
        @Suppress("UNCHECKED_CAST")
        return userIds as List<String>
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 部门-用户关系id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的部门-用户关系后，同步${CACHE_NAME}缓存...")
            val deptId = BeanKit.getProperty(any, AuthDeptUser::deptId.name) as String
            evict(deptId) // 踢除缓存，因为缓存的粒度为部门
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByDeptIdCacheHandler>().getUserIds(deptId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 部门-用户关系id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的部门-用户关系后，同步${CACHE_NAME}缓存...")
            val deptId = if (any == null) {
                authDeptUserDao.get(id)!!.deptId
            } else {
                BeanKit.getProperty(any, AuthDeptUser::deptId.name) as String
            }
            evict(deptId) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByDeptIdCacheHandler>().getUserIds(deptId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 部门-用户关系id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val deptId = BeanKit.getProperty(any, AuthDeptUser::deptId.name) as String
            log.debug("删除id为${id}的部门-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
            evict(deptId) // 踢除缓存，缓存的粒度为部门
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByDeptIdCacheHandler>().getUserIds(deptId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 部门-用户关系id集合
     * @param deptIds 部门id集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, deptIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的部门-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
            deptIds.forEach { deptId ->
                CacheKit.evict(CACHE_NAME, deptId) // 踢除缓存，缓存的粒度为部门
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<UserIdsByDeptIdCacheHandler>().getUserIds(deptId) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
