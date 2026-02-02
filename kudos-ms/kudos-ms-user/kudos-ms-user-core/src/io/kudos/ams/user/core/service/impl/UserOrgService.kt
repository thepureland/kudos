package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.org.UserOrgTreeRecord
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.cache.OrgByIdCacheHandler
import io.kudos.ms.user.core.cache.OrgIdsByTenantIdCacheHandler
import io.kudos.ms.user.core.cache.UserByIdCacheHandler
import io.kudos.ms.user.core.cache.UserIdsByOrgIdCacheHandler
import io.kudos.ms.user.core.dao.UserOrgDao
import io.kudos.ms.user.core.dao.UserOrgUserDao
import io.kudos.ms.user.core.model.po.UserOrg
import io.kudos.ms.user.core.model.po.UserOrgUser
import io.kudos.ms.user.core.service.iservice.IUserOrgService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
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
//region your codes 1
open class UserOrgService : BaseCrudService<String, UserOrg, UserOrgDao>(), IUserOrgService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdsByOrgIdCacheHandler: UserIdsByOrgIdCacheHandler

    @Autowired
    private lateinit var orgByIdCacheHandler: OrgByIdCacheHandler

    @Autowired
    private lateinit var orgIdsByTenantIdCacheHandler: OrgIdsByTenantIdCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getOrgAdmins(orgId: String): List<UserAccountCacheItem> {
        // 查询机构管理员用户ID列表
        val criteria = Criteria(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrgUser::orgAdmin.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val adminUserIds = userOrgUserDao.searchProperty(criteria, UserOrgUser::userId.name) as List<String>
        
        // 如果没有管理员，直接返回空列表
        if (adminUserIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取用户信息
        val usersMap = userByIdCacheHandler.getUsersByIds(adminUserIds)
        
        // 返回用户列表（按原始ID顺序）
        return adminUserIds.mapNotNull { usersMap[it] }
    }

    override fun getOrgUserIds(orgId: String): List<String> {
        return userIdsByOrgIdCacheHandler.getUserIds(orgId)
    }

    override fun getChildOrgIds(orgId: String): List<String> {
        val criteria = Criteria(UserOrg::parentId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrg::active.name, OperatorEnum.EQ, true)
        val childOrgs = search(criteria)
        return childOrgs.mapNotNull { it.id }
    }

    override fun getOrgUsers(orgId: String): List<UserAccountCacheItem> {
        val userIds = getOrgUserIds(orgId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val usersMap = userByIdCacheHandler.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        val userIds = getOrgUserIds(orgId)
        return userIds.contains(userId)
    }

    override fun getChildOrgs(orgId: String): List<UserOrgCacheItem> {
        val childOrgIds = getChildOrgIds(orgId)
        if (childOrgIds.isEmpty()) {
            return emptyList()
        }
        val orgsMap = orgByIdCacheHandler.getOrgsByIds(childOrgIds)
        return childOrgIds.mapNotNull { orgsMap[it] }
    }

    override fun getParentOrg(orgId: String): UserOrgCacheItem? {
        val org = orgByIdCacheHandler.getOrgById(orgId) ?: return null
        val parentId = org.parentId ?: return null
        return orgByIdCacheHandler.getOrgById(parentId)
    }

    override fun getOrgRecord(id: String): UserOrgCacheItem? {
        return orgByIdCacheHandler.getOrgById(id)
    }

    override fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheItem> {
        val orgIds = orgIdsByTenantIdCacheHandler.getOrgIds(tenantId)
        if (orgIds.isEmpty()) {
            return emptyList()
        }
        val orgsMap = orgByIdCacheHandler.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    override fun getOrgTree(tenantId: String, parentId: String?): List<UserOrgTreeRecord> {
        val criteria = Criteria(UserOrg::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(UserOrg::active.name, OperatorEnum.EQ, true)
        
        // 如果指定了parentId，只查询该父机构下的直接子机构
        // 如果没有指定parentId，查询所有机构以构建完整树
        if (parentId != null) {
            criteria.addAnd(UserOrg::parentId.name, OperatorEnum.EQ, parentId)
        }
        // 注意：当parentId == null时，不添加parent_id条件，查询所有机构
        
        val orgs = dao.search(criteria)
        
        // 转换为树节点
        val treeNodes = orgs.map { org ->
            val cacheItem = orgByIdCacheHandler.getOrgById(org.id!!) ?: return@map null
            UserOrgTreeRecord().apply {
                BeanKit.copyProperties(cacheItem, this)
                this.children = mutableListOf()
            }
        }.filterNotNull()
        
        // 如果指定了parentId，直接返回子机构列表（不构建树）
        if (parentId != null) {
            return treeNodes.sortedBy { it.sortNum ?: Int.MAX_VALUE }
        }
        
        // 构建树形结构（仅当parentId == null时）
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<UserOrgTreeRecord>()
        
        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }
        
        // 按 sortNum 排序
        fun sortTree(nodes: MutableList<UserOrgTreeRecord>) {
            nodes.sortBy { it.sortNum ?: Int.MAX_VALUE }
            nodes.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)
        
        return rootNodes
    }

    override fun getAllAncestorOrgIds(orgId: String): List<String> {
        val ancestors = mutableListOf<String>()
        var currentOrg = orgByIdCacheHandler.getOrgById(orgId) ?: return emptyList()
        
        while (currentOrg.parentId != null) {
            ancestors.add(currentOrg.parentId!!)
            currentOrg = orgByIdCacheHandler.getOrgById(currentOrg.parentId!!) ?: break
        }
        
        return ancestors
    }

    override fun getAllDescendantOrgIds(orgId: String): List<String> {
        val descendants = mutableListOf<String>()
        val queue = mutableListOf(orgId)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            val childIds = getChildOrgIds(currentId)
            descendants.addAll(childIds)
            queue.addAll(childIds)
        }
        
        return descendants
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val org = UserOrg {
            this.id = id
            this.active = active
        }
        val success = dao.update(org)
        if (success) {
            log.debug("更新id为${id}的机构的启用状态为${active}。")
            orgByIdCacheHandler.syncOnUpdate(id)
            val existingOrg = dao.get(id)
            if (existingOrg != null) {
                orgIdsByTenantIdCacheHandler.syncOnUpdateActive(id, active)
            }
        } else {
            log.error("更新id为${id}的机构的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun moveOrg(id: String, newParentId: String?, newSortNum: Int?): Boolean {
        val org = UserOrg {
            this.id = id
            this.parentId = newParentId
            this.sortNum = newSortNum
        }
        val success = dao.update(org)
        if (success) {
            log.debug("移动id为${id}的机构到父机构${newParentId}，排序号${newSortNum}。")
            orgByIdCacheHandler.syncOnUpdate(id)
            val existingOrg = dao.get(id)
            if (existingOrg != null) {
                orgIdsByTenantIdCacheHandler.syncOnUpdate(org, id)
            }
        } else {
            log.error("移动id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的机构。")
        orgByIdCacheHandler.syncOnInsert(id)
        orgIdsByTenantIdCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, UserOrg::id.name) as String
        if (success) {
            log.debug("更新id为${id}的机构。")
            orgByIdCacheHandler.syncOnUpdate(id)
            orgIdsByTenantIdCacheHandler.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val org = dao.get(id)
        if (org == null) {
            log.warn("删除id为${id}的机构时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的机构。")
            orgByIdCacheHandler.syncOnDelete(id)
            orgIdsByTenantIdCacheHandler.syncOnDelete(org, id)
        } else {
            log.error("删除id为${id}的机构失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val orgs = dao.inSearchById(ids)
        val tenantIds = orgs.map { it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除机构，期望删除${ids.size}条，实际删除${count}条。")
        orgByIdCacheHandler.syncOnBatchDelete(ids)
        orgIdsByTenantIdCacheHandler.syncOnBatchDelete(ids, tenantIds)
        return count
    }

    //endregion your codes 2

}
