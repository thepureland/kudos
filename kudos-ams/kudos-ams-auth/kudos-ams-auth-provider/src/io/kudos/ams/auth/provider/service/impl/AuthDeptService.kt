package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptTreeRecord
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.cache.DeptByIdCacheHandler
import io.kudos.ams.auth.provider.cache.DeptIdsByTenantIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserIdsByDeptIdCacheHandler
import io.kudos.ams.auth.provider.dao.AuthDeptDao
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.model.po.AuthDept
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.service.iservice.IAuthDeptService
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 部门业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthDeptService : BaseCrudService<String, AuthDept, AuthDeptDao>(), IAuthDeptService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdsByDeptIdCacheHandler: UserIdsByDeptIdCacheHandler

    @Autowired
    private lateinit var deptByIdCacheHandler: DeptByIdCacheHandler

    @Autowired
    private lateinit var deptIdsByTenantIdCacheHandler: DeptIdsByTenantIdCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getDeptAdmins(deptId: String): List<AuthUserCacheItem> {
        // 查询部门管理员用户ID列表
        val criteria = Criteria(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDeptUser::deptAdmin.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val adminUserIds = authDeptUserDao.searchProperty(criteria, AuthDeptUser::userId.name) as List<String>
        
        // 如果没有管理员，直接返回空列表
        if (adminUserIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取用户信息
        val usersMap = userByIdCacheHandler.getUsersByIds(adminUserIds)
        
        // 返回用户列表（按原始ID顺序）
        return adminUserIds.mapNotNull { usersMap[it] }
    }

    override fun getDeptUserIds(deptId: String): List<String> {
        return userIdsByDeptIdCacheHandler.getUserIds(deptId)
    }

    override fun getChildDeptIds(deptId: String): List<String> {
        val criteria = Criteria(AuthDept::parentId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDept::active.name, OperatorEnum.EQ, true)
        val childDepts = search(criteria)
        return childDepts.mapNotNull { it.id }
    }

    override fun getDeptUsers(deptId: String): List<AuthUserCacheItem> {
        val userIds = getDeptUserIds(deptId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val usersMap = userByIdCacheHandler.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    override fun isUserInDept(userId: String, deptId: String): Boolean {
        val userIds = getDeptUserIds(deptId)
        return userIds.contains(userId)
    }

    override fun getChildDepts(deptId: String): List<AuthDeptCacheItem> {
        val childDeptIds = getChildDeptIds(deptId)
        if (childDeptIds.isEmpty()) {
            return emptyList()
        }
        val deptsMap = deptByIdCacheHandler.getDeptsByIds(childDeptIds)
        return childDeptIds.mapNotNull { deptsMap[it] }
    }

    override fun getParentDept(deptId: String): AuthDeptCacheItem? {
        val dept = deptByIdCacheHandler.getDeptById(deptId) ?: return null
        val parentId = dept.parentId ?: return null
        return deptByIdCacheHandler.getDeptById(parentId)
    }

    override fun getDeptRecord(id: String): AuthDeptCacheItem? {
        return deptByIdCacheHandler.getDeptById(id)
    }

    override fun getDeptsByTenantId(tenantId: String): List<AuthDeptCacheItem> {
        val deptIds = deptIdsByTenantIdCacheHandler.getDeptIds(tenantId)
        if (deptIds.isEmpty()) {
            return emptyList()
        }
        val deptsMap = deptByIdCacheHandler.getDeptsByIds(deptIds)
        return deptIds.mapNotNull { deptsMap[it] }
    }

    override fun getDeptTree(tenantId: String, parentId: String?): List<AuthDeptTreeRecord> {
        val criteria = Criteria(AuthDept::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthDept::active.name, OperatorEnum.EQ, true)
        
        // 如果指定了parentId，只查询该父部门下的直接子部门
        // 如果没有指定parentId，查询所有部门以构建完整树
        if (parentId != null) {
            criteria.addAnd(AuthDept::parentId.name, OperatorEnum.EQ, parentId)
        }
        // 注意：当parentId == null时，不添加parent_id条件，查询所有部门
        
        val depts = dao.search(criteria)
        
        // 转换为树节点
        val treeNodes = depts.map { dept ->
            val cacheItem = deptByIdCacheHandler.getDeptById(dept.id!!) ?: return@map null
            AuthDeptTreeRecord().apply {
                BeanKit.copyProperties(cacheItem, this)
                this.children = mutableListOf()
            }
        }.filterNotNull()
        
        // 如果指定了parentId，直接返回子部门列表（不构建树）
        if (parentId != null) {
            return treeNodes.sortedBy { it.sortNum ?: Int.MAX_VALUE }
        }
        
        // 构建树形结构（仅当parentId == null时）
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<AuthDeptTreeRecord>()
        
        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }
        
        // 按 sortNum 排序
        fun sortTree(nodes: MutableList<AuthDeptTreeRecord>) {
            nodes.sortBy { it.sortNum ?: Int.MAX_VALUE }
            nodes.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)
        
        return rootNodes
    }

    override fun getAllAncestorDeptIds(deptId: String): List<String> {
        val ancestors = mutableListOf<String>()
        var currentDept = deptByIdCacheHandler.getDeptById(deptId) ?: return emptyList()
        
        while (currentDept.parentId != null) {
            ancestors.add(currentDept.parentId!!)
            currentDept = deptByIdCacheHandler.getDeptById(currentDept.parentId!!) ?: break
        }
        
        return ancestors
    }

    override fun getAllDescendantDeptIds(deptId: String): List<String> {
        val descendants = mutableListOf<String>()
        val queue = mutableListOf(deptId)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            val childIds = getChildDeptIds(currentId)
            descendants.addAll(childIds)
            queue.addAll(childIds)
        }
        
        return descendants
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dept = AuthDept {
            this.id = id
            this.active = active
        }
        val success = dao.update(dept)
        if (success) {
            log.debug("更新id为${id}的部门的启用状态为${active}。")
            deptByIdCacheHandler.syncOnUpdate(id)
            val existingDept = dao.get(id)
            if (existingDept != null) {
                deptIdsByTenantIdCacheHandler.syncOnUpdateActive(id, active)
            }
        } else {
            log.error("更新id为${id}的部门的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun moveDept(id: String, newParentId: String?, newSortNum: Int?): Boolean {
        val dept = AuthDept {
            this.id = id
            this.parentId = newParentId
            this.sortNum = newSortNum
        }
        val success = dao.update(dept)
        if (success) {
            log.debug("移动id为${id}的部门到父部门${newParentId}，排序号${newSortNum}。")
            deptByIdCacheHandler.syncOnUpdate(id)
            val existingDept = dao.get(id)
            if (existingDept != null) {
                deptIdsByTenantIdCacheHandler.syncOnUpdate(dept, id)
            }
        } else {
            log.error("移动id为${id}的部门失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的部门。")
        deptByIdCacheHandler.syncOnInsert(id)
        deptIdsByTenantIdCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthDept::id.name) as String
        if (success) {
            log.debug("更新id为${id}的部门。")
            deptByIdCacheHandler.syncOnUpdate(id)
            deptIdsByTenantIdCacheHandler.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的部门失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val dept = dao.get(id)
        if (dept == null) {
            log.warn("删除id为${id}的部门时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的部门。")
            deptByIdCacheHandler.syncOnDelete(id)
            deptIdsByTenantIdCacheHandler.syncOnDelete(dept, id)
        } else {
            log.error("删除id为${id}的部门失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val depts = dao.inSearchById(ids)
        val tenantIds = depts.map { it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除部门，期望删除${ids.size}条，实际删除${count}条。")
        deptByIdCacheHandler.syncOnBatchDelete(ids)
        deptIdsByTenantIdCacheHandler.syncOnBatchDelete(ids, tenantIds)
        return count
    }

    //endregion your codes 2

}
