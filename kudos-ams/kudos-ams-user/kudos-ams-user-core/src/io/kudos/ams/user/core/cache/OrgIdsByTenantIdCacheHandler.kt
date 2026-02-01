package io.kudos.ams.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.user.core.dao.UserOrgDao
import io.kudos.ams.user.core.model.po.UserOrg
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.IIdEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 机构ID列表（by tenant id）缓存处理器
 *
 * 1.数据来源表：user_org
 * 2.缓存各租户下的机构ID，只包含active=true的
 * 3.缓存的key为：tenantId
 * 4.缓存的value为：机构ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class OrgIdsByTenantIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgDao: UserOrgDao

    companion object Companion {
        private const val CACHE_NAME = "USER_ORG_IDS_BY_TENANT_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有租户下的机构ID！")
            return
        }

        // 加载所有active=true的机构，按租户分组
        val criteria = Criteria(UserOrg::active.name, OperatorEnum.EQ, true)

        @Suppress("UNCHECKED_CAST")
        val allActiveOrgs = userOrgDao.search(criteria)
        val tenantIdAndOrgIdsMap = allActiveOrgs
            .groupBy { it.tenantId }
            .mapValues { entry -> entry.value.map { it.id!! } }

        log.debug("从数据库加载了${allActiveOrgs.size}条机构信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存机构ID
        tenantIdAndOrgIdsMap.forEach { (tenantId, orgIds) ->
            CacheKit.put(CACHE_NAME, tenantId, orgIds)
            log.debug("缓存了租户${tenantId}的${orgIds.size}条机构ID。")
        }
    }

    /**
     * 根据租户ID从缓存中获取其下所有机构ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param tenantId 租户ID
     * @return List<机构ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getOrgIds(tenantId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}的机构ID，从数据库中加载...")
        }

        val criteria = Criteria(UserOrg::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(UserOrg::active.name, OperatorEnum.EQ, true)
        val orgIds = userOrgDao.searchProperty(criteria, IIdEntity<*>::id.name)
        log.debug("从数据库加载了租户${tenantId}的${orgIds.size}条机构ID。")
        @Suppress("UNCHECKED_CAST")
        return orgIds as List<String>
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的机构后，同步${CACHE_NAME}缓存...")
            val tenantId = BeanKit.getProperty(any, UserOrg::tenantId.name) as String
            evict(tenantId) // 踢除缓存，因为缓存的粒度为租户
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(tenantId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的机构后，同步${CACHE_NAME}缓存...")
            val tenantId = if (any == null) {
                userOrgDao.get(id)!!.tenantId
            } else {
                BeanKit.getProperty(any, UserOrg::tenantId.name) as String
            }
            evict(tenantId) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(tenantId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 机构id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的机构的启用状态后，同步缓存...")
            val userOrg = userOrgDao.get(id)!!
            evict(userOrg.tenantId) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(userOrg.tenantId) // 重新缓存
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 机构id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val tenantId = BeanKit.getProperty(any, UserOrg::tenantId.name) as String
            log.debug("删除id为${id}的机构后，同步从${CACHE_NAME}缓存中踢除...")
            evict(tenantId) // 踢除缓存，缓存的粒度为租户
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(tenantId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 机构id集合
     * @param tenantIds 租户id集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, tenantIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的机构后，同步从${CACHE_NAME}缓存中踢除...")
            tenantIds.forEach { tenantId ->
                CacheKit.evict(CACHE_NAME, tenantId) // 踢除缓存，缓存的粒度为租户
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<OrgIdsByTenantIdCacheHandler>().getOrgIds(tenantId) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
