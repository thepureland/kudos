package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ms.auth.core.dao.AuthRoleDao
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 角色ID（by tenant id & role code）缓存处理器
 *
 * 1.数据来源表：auth_role
 * 2.缓存所有active=true的角色ID
 * 3.缓存的key为：tenantId::roleCode
 * 4.缓存的value为：角色ID（String）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class RoleIdByTenantIdAndRoleCodeCache : AbstractKeyValueCacheHandler<String>() {

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    companion object Companion {
        private const val CACHE_NAME = "AUTH_ROLE_ID_BY_TENANT_ID_AND_ROLE_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): String? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}角色编码"
        }
        val tenantAndCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<RoleIdByTenantIdAndRoleCodeCache>().getRoleId(
            tenantAndCode[0], tenantAndCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的角色ID！")
            return
        }

        // 加载所有可用的角色
        val criteria = Criteria(AuthRole::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val roles = authRoleDao.search(criteria)
        log.debug("从数据库加载了${roles.size}条角色信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存角色ID
        roles.forEach {
            CacheKit.put(CACHE_NAME, getKey(it.tenantId, it.code), it.id!!)
        }
        log.debug("缓存了${roles.size}条角色ID信息。")
    }

    /**
     * 根据租户ID和角色编码从缓存获取对应的角色ID，如果缓存中不存在，则从数据库中加载，并写回缓存
     *
     * @param tenantId 租户ID
     * @param code 角色编码
     * @return 角色ID，找不到返回null
     */
    @Cacheable(
        value = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#code)",
        unless = "#result == null"
    )
    open fun getRoleId(tenantId: String, code: String): String? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}且角色编码为${code}的角色ID，从数据库中加载...")
        }

        val criteria = Criteria(AuthRole::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthRole::code.name, OperatorEnum.EQ, code)
            .addAnd(AuthRole::active.name, OperatorEnum.EQ, true)
        
        @Suppress("UNCHECKED_CAST")
        val roles = authRoleDao.search(criteria)
        return if (roles.isEmpty()) {
            log.debug("从数据库找不到租户${tenantId}且角色编码为${code}的active=true的角色。")
            null
        } else {
            log.debug("从数据库加载了租户${tenantId}且角色编码为${code}的角色ID。")
            roles.first().id
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 角色id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的角色后，同步${CACHE_NAME}缓存...")
            val tenantId = BeanKit.getProperty(any, AuthRole::tenantId.name) as String
            val code = BeanKit.getProperty(any, AuthRole::code.name) as String
            CacheKit.put(CACHE_NAME, getKey(tenantId, code), id) // 直接缓存ID
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 角色id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的角色后，同步${CACHE_NAME}缓存...")
            val role = if (any == null) {
                authRoleDao.get(id)!!
            } else {
                val tenantId = BeanKit.getProperty(any, AuthRole::tenantId.name) as String
                val code = BeanKit.getProperty(any, AuthRole::code.name) as String
                AuthRole().apply {
                    this.id = id
                    this.tenantId = tenantId
                    this.code = code
                }
            }
            CacheKit.evict(CACHE_NAME, getKey(role.tenantId, role.code)) // 踢除缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RoleIdByTenantIdAndRoleCodeCache>().getRoleId(role.tenantId, role.code) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 角色id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的角色的启用状态后，同步缓存...")
            val authRole = authRoleDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    CacheKit.put(CACHE_NAME, getKey(authRole.tenantId, authRole.code), id)
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(authRole.tenantId, authRole.code)) // 踢除缓存
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 角色id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val tenantId = BeanKit.getProperty(any, AuthRole::tenantId.name) as String
            val code = BeanKit.getProperty(any, AuthRole::code.name) as String
            log.debug("删除id为${id}的角色后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, code)) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 角色id集合
     * @param tenantAndCodes List<Pair<租户ID，角色编码>>
     */
    open fun syncOnBatchDelete(ids: Collection<String>, tenantAndCodes: List<Pair<String, String>>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的角色后，同步从${CACHE_NAME}缓存中踢除...")
            tenantAndCodes.forEach {
                CacheKit.evict(CACHE_NAME, getKey(it.first, it.second)) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户ID
     * @param code 角色编码
     * @return 缓存key
     */
    fun getKey(tenantId: String, code: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${code}"
    }

    private val log = LogFactory.getLog(this)

}
