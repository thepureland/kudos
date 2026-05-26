package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for user-id lists keyed by tenant id + role code.
 *
 * **Semantics: "effective user set"** (parallel to [UserIdsByRoleIdCache]):
 * - Source: `auth_role` UNION `auth_role_user` (direct) UNION `auth_group_role` + `auth_group_user` (group inheritance).
 * - The cached value is the deduplicated union of direct-binding + group-inherited user IDs.
 *
 * Cache key: tenantId::roleCode; value: List<String>.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByTenantIdAndRoleCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_TENANT_ID_AND_ROLE_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "key format for cache ${CACHE_NAME} must be tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}roleCode"
        }
        val tenantAndRoleCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<UserIdsByTenantIdAndRoleCodeCache>().getUserIds(
            tenantAndRoleCode[0], tenantAndRoleCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache not enabled; skip loading and caching user IDs for all roles!")
            return
        }

        val roles = authRoleDao.searchActiveRolesForCache()
        val roleIdToDirectUserIds = authRoleUserDao.getAllRoleIdToUserIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val groupIdToUserIds = authGroupUserDao.searchAllGroupIdToUserIdsForCache()
        val roleIdToGroupIds: Map<String, List<String>> = buildMap<String, MutableList<String>> {
            groupIdToRoleIds.forEach { (groupId, roleIds) ->
                roleIds.forEach { roleId -> getOrPut(roleId) { mutableListOf() }.add(groupId) }
            }
        }

        log.debug(
            "Loaded ${roles.size} roles from DB; direct role-user groupings: ${roleIdToDirectUserIds.size}; " +
                "group-role groupings: ${groupIdToRoleIds.size}; group-user groupings: ${groupIdToUserIds.size}."
        )

        if (clear) {
            clear()
        }

        roles.forEach { role ->
            val roleId = role.id
            if (roleId.isBlank()) return@forEach
            val tenantId = role.tenantId ?: return@forEach
            val roleCode = role.code ?: return@forEach
            val direct = roleIdToDirectUserIds[roleId].orEmpty()
            val viaGroup = roleIdToGroupIds[roleId].orEmpty().flatMap { gid ->
                groupIdToUserIds[gid].orEmpty()
            }
            val all = (direct + viaGroup).distinct()
            KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, roleCode), all)
            log.debug(
                "Cached ${all.size} user IDs for tenant ${tenantId} role ${roleCode} (direct ${direct.size} + group-inherited ${viaGroup.size})."
            )
        }
    }

    /**
     * Returns all user IDs under the given tenant + role code from the cache
     * (direct binding UNION group inheritance).
     *
     * @param tenantId tenant id
     * @param roleCode role code
     * @return List<user id>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#roleCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(tenantId: String, roleCode: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No cached user IDs for tenant ${tenantId} role ${roleCode}; loading from DB...")
        }

        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
        if (roleId == null) {
            log.debug("Role ${roleCode} not found for tenant ${tenantId}.")
            return emptyList()
        }

        val direct = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }
        }
        val effective = (direct + viaGroup).distinct()
        log.debug(
            "Loaded effective users for tenant ${tenantId} role ${roleCode} from DB: direct ${direct.size}, group-inherited ${viaGroup.size}, deduplicated ${effective.size}."
        )
        return effective
    }

    /**
     * Sync the cache after a role-user relation insert (backward-compatible entry; new code should use
     * the event [AuthRoleUserRelationsChanged]).
     */
    open fun syncOnRoleUserInsert(tenantId: String, roleCode: String) = syncOnRoleUserDelete(tenantId, roleCode)

    /**
     * Sync the cache after a role-user relation delete (backward-compatible entry; new code should use events).
     */
    open fun syncOnRoleUserDelete(tenantId: String, roleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("After role-user relation change for tenant ${tenantId} role ${roleCode}, syncing ${CACHE_NAME} cache...")
        evict(getKey(tenantId, roleCode))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<UserIdsByTenantIdAndRoleCodeCache>().getUserIds(tenantId, roleCode)
        }
        log.debug("${CACHE_NAME} cache sync complete.")
    }

    /**
     * Sync the cache after role info is updated (role code or tenant changed).
     */
    open fun syncOnRoleUpdate(oldTenantId: String, oldRoleCode: String, newTenantId: String, newRoleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("After role info update, syncing ${CACHE_NAME} cache...")

        KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldRoleCode))
        if (oldTenantId != newTenantId || oldRoleCode != newRoleCode) {
            KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newRoleCode))
        }
        log.debug("${CACHE_NAME} cache sync complete.")
    }

    fun getKey(tenantId: String, roleCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${roleCode}"
    }

    /** Local (tenantId, roleCode) eviction only; AuthRoleHashCache subscribes to AuthRoleDeleted independently. */
    private fun evictBy(tenantId: String, roleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, roleCode))
    }

    /**
     * Resolves (tenantId, code) for a set of roleIds and evicts them. One hash-cache lookup per
     * roleId — typically only a few dozen entries, which is acceptable; optimize further if batch
     * sizes grow.
     */
    private fun evictByRoleIds(roleIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        roleIds.forEach { roleId ->
            val role = authRoleHashCache.getRoleById(roleId)
            val tenantId = role?.tenantId
            val code = role?.code
            if (tenantId != null && code != null) {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, code))
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictBy(event.tenantId, event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.code) }
    }

    /** Direct role-user relation changes => evict the (tenantId, code) tied to this roleId. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = evictByRoleIds(listOf(event.roleId))

    /** A user joining/leaving a group => evict (tenantId, code) for every roleId tied to that group. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(event.groupId)
        if (roleIds.isEmpty()) return
        evictByRoleIds(roleIds)
    }

    /** The roles bound to a group changed => evict (tenantId, code) for those roles. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit = evictByRoleIds(event.roleIds)

    private val log = LogFactory.getLog(this::class)

}
