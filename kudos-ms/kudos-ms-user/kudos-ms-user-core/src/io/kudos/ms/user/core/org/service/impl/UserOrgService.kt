package io.kudos.ms.user.core.org.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgInserted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 机构业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserOrgService(
    dao: UserOrgDao
) : BaseCrudService<String, UserOrg, UserOrgDao>(dao), IUserOrgService {


    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Autowired
    private lateinit var userIdsByOrgIdCache: UserIdsByOrgIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> {
        val adminUserIds = userOrgUserDao.searchAdminUserIdsByOrgId(orgId)
        if (adminUserIds.isEmpty()) return emptyList()
        // 批量获取用户信息，按原始 ID 顺序返回
        val usersMap = userAccountHashCache.getUsersByIds(adminUserIds)
        return adminUserIds.mapNotNull { usersMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getOrgUserIds(orgId: String): List<String> = userIdsByOrgIdCache.getUserIds(orgId)

    @Transactional(readOnly = true)
    override fun getChildOrgIds(orgId: String): List<String> = dao.searchActiveChildOrgIds(orgId)

    @Transactional(readOnly = true)
    override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> {
        val userIds = getOrgUserIds(orgId)
        if (userIds.isEmpty()) return emptyList()
        val usersMap = userAccountHashCache.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    @Transactional(readOnly = true)
    override fun isUserInOrg(userId: String, orgId: String): Boolean = userId in getOrgUserIds(orgId)

    @Transactional(readOnly = true)
    override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> {
        val childOrgIds = getChildOrgIds(orgId)
        if (childOrgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(childOrgIds)
        return childOrgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getParentOrg(orgId: String): UserOrgCacheEntry? =
        userOrgHashCache.getOrgById(orgId)?.parentId?.let { userOrgHashCache.getOrgById(it) }

    @Transactional(readOnly = true)
    override fun getOrgRecord(id: String): UserOrgCacheEntry? = userOrgHashCache.getOrgById(id)

    @Transactional(readOnly = true)
    override fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheEntry> {
        val orgIds = userOrgHashCache.getOrgsByTenantId(tenantId).map { it.id }
        if (orgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getOrgTree(tenantId: String, parentId: String?): List<UserOrgTreeRow> {
        // 如果指定了parentId，只查询该父机构下的直接子机构；否则查询租户下全部启用机构
        val orgs = dao.searchActiveOrgsByTenantId(tenantId, parentId)
        
        // 转换为树节点（UserOrgTreeRow 为不可变 data class，全部 val，用构造器传值；BeanKit.copyProperties 走 setter
        // 在此处不可用，会留下空 row）
        val treeNodes = orgs.mapNotNull { org ->
            val cacheItem = userOrgHashCache.getOrgById(org.id) ?: return@mapNotNull null
            UserOrgTreeRow(
                id = cacheItem.id,
                name = cacheItem.name,
                shortName = cacheItem.shortName,
                tenantId = cacheItem.tenantId,
                parentId = cacheItem.parentId,
                orgTypeDictCode = cacheItem.orgTypeDictCode,
                sortNum = cacheItem.sortNum,
                remark = cacheItem.remark,
                active = cacheItem.active,
                builtIn = cacheItem.builtIn,
                createUserId = cacheItem.createUserId,
                createUserName = cacheItem.createUserName,
                createTime = cacheItem.createTime,
                updateUserId = cacheItem.updateUserId,
                updateUserName = cacheItem.updateUserName,
                updateTime = cacheItem.updateTime,
                children = mutableListOf(),
            )
        }
        
        // 如果指定了parentId，直接返回子机构列表（不构建树）
        if (parentId != null) {
            return treeNodes.sortedBy { it.sortNum ?: Int.MAX_VALUE }
        }
        
        // 构建树形结构（仅当parentId == null时）
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<UserOrgTreeRow>()
        
        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }
        
        // 按 sortNum 排序
        fun sortTree(nodes: MutableList<UserOrgTreeRow>) {
            nodes.sortBy { it.sortNum ?: Int.MAX_VALUE }
            nodes.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)
        
        return rootNodes
    }

    @Transactional(readOnly = true)
    override fun getAllAncestorOrgIds(orgId: String): List<String> {
        val ancestors = mutableListOf<String>()
        var currentOrg = userOrgHashCache.getOrgById(orgId) ?: return emptyList()
        while (true) {
            val parentId = currentOrg.parentId ?: break
            ancestors.add(parentId)
            // 缓存命中失败时仍保留刚加进去的 parentId（祖先链可能跨越缓存边界，不能整段截掉）
            currentOrg = userOrgHashCache.getOrgById(parentId) ?: break
        }
        return ancestors
    }

    @Transactional(readOnly = true)
    override fun getAllDescendantOrgIds(orgId: String): List<String> {
        val descendants = mutableListOf<String>()
        // ArrayDeque.removeFirst 是 O(1)，避免 MutableList.removeAt(0) 每次 O(n) 搬移
        val queue = ArrayDeque(listOf(orgId))
        while (queue.isNotEmpty()) {
            val childIds = getChildOrgIds(queue.removeFirst())
            descendants.addAll(childIds)
            queue.addAll(childIds)
        }
        return descendants
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        // active 改了会影响"父机构含子树成员"视图（子机构禁用 → 父级视图里这棵子树就该没人了）。
        // 因此即使 parentId 没动，也要把 parentId 快照塞进事件，让 listener 沿祖先链清缓存。
        val parentId = dao.get(id)?.parentId
        val success = dao.updateProperties(id, mapOf(UserOrg::active.name to active))
        if (success) {
            log.debug("更新id为${id}的机构的启用状态为${active}。")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = parentId, newParentId = parentId))
        } else {
            log.error("更新id为${id}的机构的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun moveOrg(id: String, newParentId: String?, newSortNum: Int?): Boolean {
        // 移动前先 snapshot oldParentId —— 事务提交后 dao 看不到旧值。
        val oldParentId = dao.get(id)?.parentId
        val props = mutableMapOf<String, Any?>(UserOrg::parentId.name to newParentId)
        newSortNum?.let { props[UserOrg::sortNum.name] = it }
        val success = dao.updateProperties(id, props)
        if (success) {
            log.debug("移动id为${id}的机构到父机构${newParentId}，排序号${newSortNum}。")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = oldParentId, newParentId = newParentId))
        } else {
            log.error("移动id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的机构。")
        eventPublisher.publishEvent(UserOrgInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        // 通用 update：snapshot 事前 parentId，update 之后再读事后 parentId
        val id = BeanKit.getProperty(any, UserOrg::id.name) as String
        val oldParentId = dao.get(id)?.parentId
        val success = super.update(any)
        if (success) {
            val newParentId = dao.get(id)?.parentId
            log.debug("更新id为${id}的机构。")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = oldParentId, newParentId = newParentId))
        } else {
            log.error("更新id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val org = dao.get(id) ?: run {
            log.warn("删除id为${id}的机构时，发现其已不存在！")
            return false
        }
        val parentIdSnapshot = org.parentId
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的机构。")
            eventPublisher.publishEvent(UserOrgDeleted(id, parentId = parentIdSnapshot))
        } else {
            log.error("删除id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // 先 snapshot (id, parentId)，AFTER_COMMIT 时行已删除，listener 无法回查
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { UserOrgBatchDeleted.Item(it.id, it.parentId) }
        val count = super.batchDelete(ids)
        log.debug("批量删除机构，期望删除${ids.size}条，实际删除${count}条。")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(UserOrgBatchDeleted(snapshots))
        }
        return count
    }


}
