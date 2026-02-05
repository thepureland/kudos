package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.user.core.dao.UserOrgUserDao
import io.kudos.ms.user.core.model.po.UserOrgUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户ID列表（by org）缓存处理器
 *
 * 1.数据来源表：user_org_user
 * 2.缓存各机构下的用户ID
 * 3.缓存的key为：orgId
 * 4.缓存的value为：用户ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByOrgIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    companion object {
        private const val CACHE_NAME = "USER_IDS_BY_ORG_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByOrgIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有机构下的用户ID！")
            return
        }

        // 加载所有机构-用户关系，按机构分组
        @Suppress("UNCHECKED_CAST")
        val allOrgUsers = userOrgUserDao.allSearch()
        val orgIdAndUserIdsMap = allOrgUsers
            .groupBy { it.orgId }
            .mapValues { entry -> entry.value.map { it.userId } }

        log.debug("从数据库加载了${allOrgUsers.size}条机构-用户关系信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户ID
        orgIdAndUserIdsMap.forEach { (orgId, userIds) ->
            CacheKit.put(CACHE_NAME, orgId, userIds)
            log.debug("缓存了机构${orgId}的${userIds.size}条用户ID。")
        }
    }

    /**
     * 根据机构ID从缓存中获取其下所有用户ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param orgId 机构ID
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#orgId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(orgId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在机构${orgId}的用户ID，从数据库中加载...")
        }

        val criteria = Criteria(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
        val userIds = userOrgUserDao.searchProperty(criteria, UserOrgUser::userId.name)
        log.debug("从数据库加载了机构${orgId}的${userIds.size}条用户ID。")
        @Suppress("UNCHECKED_CAST")
        return userIds as List<String>
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构-用户关系id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的机构-用户关系后，同步${CACHE_NAME}缓存...")
            val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
            evict(orgId) // 踢除缓存，因为缓存的粒度为机构
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(orgId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构-用户关系id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的机构-用户关系后，同步${CACHE_NAME}缓存...")
            val orgId = if (any == null) {
                userOrgUserDao.get(id)!!.orgId
            } else {
                BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
            }
            evict(orgId) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(orgId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构-用户关系id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
            log.debug("删除id为${id}的机构-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
            evict(orgId) // 踢除缓存，缓存的粒度为机构
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(orgId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 机构-用户关系id集合
     * @param orgIds 机构id集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, orgIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的机构-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
            orgIds.forEach { orgId ->
                CacheKit.evict(CACHE_NAME, orgId) // 踢除缓存，缓存的粒度为机构
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<UserIdsByOrgIdCache>().getUserIds(orgId) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 机构-用户关系变更后同步缓存
     *
     * @param orgId 机构ID
     */
    open fun syncOnOrgUserChange(orgId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("机构${orgId}的用户关系变更后，同步${CACHE_NAME}缓存...")
            evict(orgId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(orgId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
