package io.kudos.ams.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.user.core.dao.UserOrgUserDao
import io.kudos.ams.user.core.dao.UserAccountDao
import io.kudos.ams.user.core.model.po.UserOrgUser
import io.kudos.ams.user.core.model.po.UserAccount
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 机构ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：user_org_user
 * 2.缓存各用户所属的所有机构ID列表
 * 3.缓存的key为：userId
 * 4.缓存的value为：机构ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class OrgIdsByUserIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "USER_ORG_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<OrgIdsByUserIdCacheHandler>().getOrgIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的机构ID！")
            return
        }

        // 加载所有active=true的用户
        val userCriteria = Criteria(UserAccount::active.name, OperatorEnum.EQ, true)
        val users = userAccountDao.search(userCriteria)
        
        // 加载所有机构-用户关系
        @Suppress("UNCHECKED_CAST")
        val allOrgUsers = userOrgUserDao.allSearch()
        val userIdToOrgIdsMap = allOrgUsers
            .groupBy { it.userId }
            .mapValues { entry -> entry.value.map { it.orgId } }

        log.debug("从数据库加载了${users.size}条用户、${allOrgUsers.size}条机构-用户关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户机构ID列表
        users.forEach { user ->
            val orgIds = userIdToOrgIdsMap[user.id!!] ?: emptyList()
            if (orgIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, user.id!!, orgIds)
                log.debug("缓存了用户${user.id}的${orgIds.size}条机构ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户所属的所有机构ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param userId 用户ID
     * @return List<机构ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getOrgIds(userId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的机构ID，从数据库中加载...")
        }

        val orgUserCriteria = Criteria(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        val orgIds = userOrgUserDao.searchProperty(orgUserCriteria, UserOrgUser::orgId.name)
        
        log.debug("从数据库加载了用户${userId}的${orgIds.size}条机构ID。")
        @Suppress("UNCHECKED_CAST")
        return orgIds as List<String>
    }

    /**
     * 用户-机构关系变更后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnOrgUserChange(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${userId}的机构关系变更后，同步${CACHE_NAME}缓存...")
            evict(userId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByUserIdCacheHandler>().getOrgIds(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户-机构关系变更后同步缓存
     *
     * @param userIds 用户ID集合
     */
    open fun syncOnBatchOrgUserChange(userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户机构关系变更后，同步${CACHE_NAME}缓存...")
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<OrgIdsByUserIdCacheHandler>().getOrgIds(userId)
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
