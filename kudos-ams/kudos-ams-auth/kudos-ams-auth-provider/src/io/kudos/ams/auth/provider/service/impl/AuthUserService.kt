package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserRecord
import io.kudos.ams.auth.common.vo.user.AuthUserSearchPayload
import io.kudos.ams.auth.provider.cache.DeptByIdCacheHandler
import io.kudos.ams.auth.provider.cache.ResourceIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleByIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.cache.DeptIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserIdByTenantIdAndUsernameCacheHandler
import io.kudos.ams.auth.provider.cache.UserIdsByTenantIdAndRoleCodeCacheHandler
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.ams.auth.provider.service.iservice.IAuthUserService
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.sys.provider.cache.ResourceByIdCacheHandler
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.security.CryptoKit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 用户业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthUserService : BaseCrudService<String, AuthUser, AuthUserDao>(), IAuthUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var resourceIdsByUserIdCacheHandler: ResourceIdsByUserIdCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    @Autowired
    private lateinit var roleIdsByUserIdCacheHandler: RoleIdsByUserIdCacheHandler

    @Autowired
    private lateinit var deptIdsByUserIdCacheHandler: DeptIdsByUserIdCacheHandler

    @Autowired
    private lateinit var roleByIdCacheHandler: RoleByIdCacheHandler

    @Autowired
    private lateinit var deptByIdCacheHandler: DeptByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Autowired
    private lateinit var userIdsByTenantIdAndRoleCodeCacheHandler: UserIdsByTenantIdAndRoleCodeCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        // 通过用户ID获取资源ID列表
        val resourceIds = resourceIdsByUserIdCacheHandler.getResourceIds(userId)
        
        // 如果没有资源，返回空列表
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取资源缓存对象
        val resourcesMap = resourceByIdCacheHandler.getResourcesByIds(resourceIds)
        
        // 返回资源列表（按原始ID顺序）
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    override fun getUserRoleIds(userId: String): List<String> {
        return roleIdsByUserIdCacheHandler.getRoleIds(userId)
    }

    override fun getUserDeptIds(userId: String): List<String> {
        return deptIdsByUserIdCacheHandler.getDeptIds(userId)
    }

    override fun getUserResourceIds(userId: String): List<String> {
        return resourceIdsByUserIdCacheHandler.getResourceIds(userId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        val criteria = Criteria(AuthUser::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthUser::active.name, OperatorEnum.EQ, true)
        val users = search(criteria)
        return users.mapNotNull { it.id }
    }

    override fun getUserRoles(userId: String): List<AuthRoleCacheItem> {
        val roleIds = getUserRoleIds(userId)
        if (roleIds.isEmpty()) {
            return emptyList()
        }
        val rolesMap = roleByIdCacheHandler.getRolesByIds(roleIds)
        return roleIds.mapNotNull { rolesMap[it] }
    }

    override fun getUserDepts(userId: String): List<AuthDeptCacheItem> {
        val deptIds = getUserDeptIds(userId)
        if (deptIds.isEmpty()) {
            return emptyList()
        }
        val deptsMap = deptByIdCacheHandler.getDeptsByIds(deptIds)
        return deptIds.mapNotNull { deptsMap[it] }
    }

    override fun hasRole(userId: String, roleId: String): Boolean {
        val roleIds = getUserRoleIds(userId)
        return roleIds.contains(roleId)
    }

    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean {
        val roleId = roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, roleCode)
        return roleId != null && hasRole(userId, roleId)
    }

    override fun isUserInDept(userId: String, deptId: String): Boolean {
        val deptIds = getUserDeptIds(userId)
        return deptIds.contains(deptId)
    }

    override fun hasResource(userId: String, resourceId: String): Boolean {
        val resourceIds = getUserResourceIds(userId)
        return resourceIds.contains(resourceId)
    }

    override fun getUserByTenantIdAndUsername(tenantId: String, username: String): AuthUserCacheItem? {
        val userId = userIdByTenantIdAndUsernameCacheHandler.getUserId(tenantId, username)
        return userId?.let { userByIdCacheHandler.getUserById(it) }
    }

    override fun getUserRecord(id: String): AuthUserRecord? {
        val user = dao.get(id) ?: return null
        val record = AuthUserRecord()
        BeanKit.copyProperties(user, record)
        return record
    }

    override fun getUsersByTenantId(tenantId: String): List<AuthUserRecord> {
        val searchPayload = AuthUserSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<AuthUserRecord>
    }

    override fun getUsersByDeptId(deptId: String): List<AuthUserRecord> {
        val searchPayload = AuthUserSearchPayload().apply {
            this.deptId = deptId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<AuthUserRecord>
    }

    override fun getUsersByRoleCode(tenantId: String, roleCode: String): List<AuthUserRecord> {
        val userIds = userIdsByTenantIdAndRoleCodeCacheHandler.getUserIds(tenantId, roleCode)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val criteria = Criteria.of(AuthUser::id.name, OperatorEnum.IN, userIds)
        val users = dao.search(criteria)
        return users.map { user ->
            AuthUserRecord().apply {
                BeanKit.copyProperties(user, this)
            }
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val user = AuthUser {
            this.id = id
            this.active = active
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的启用状态为${active}。")
            userByIdCacheHandler.syncOnUpdate(id)
            val existingUser = dao.get(id)
            if (existingUser != null) {
                userIdByTenantIdAndUsernameCacheHandler.syncOnUpdateActive(id, active)
            }
        } else {
            log.error("更新id为${id}的用户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = CryptoKit.aesEncrypt(newPassword)
        val user = AuthUser {
            this.id = id
            this.loginPassword = encryptedPassword
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的登录密码。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的登录密码失败！")
        }
        return success
    }

    @Transactional
    override fun resetSecurityPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = CryptoKit.aesEncrypt(newPassword)
        val user = AuthUser {
            this.id = id
            this.securityPassword = encryptedPassword
            this.securityPasswordErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的安全密码。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的安全密码失败！")
        }
        return success
    }

    @Transactional
    override fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean {
        val user = AuthUser {
            this.id = id
            this.lastLoginIp = loginIp
            this.lastLoginTime = loginTime
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的最后登录信息。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的用户的最后登录信息失败！")
        }
        return success
    }

    @Transactional
    override fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean {
        val user = AuthUser {
            this.id = id
            this.lastLogoutTime = logoutTime
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的最后登出信息。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的用户的最后登出信息失败！")
        }
        return success
    }

    @Transactional
    override fun incrementLoginErrorTimes(id: String): Boolean {
        val existingUser = dao.get(id) ?: return false
        val currentErrorTimes = existingUser.loginErrorTimes ?: 0
        val user = AuthUser {
            this.id = id
            this.loginErrorTimes = currentErrorTimes + 1
        }
        val success = dao.update(user)
        if (success) {
            log.debug("增加id为${id}的用户的登录错误次数。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("增加id为${id}的用户的登录错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun resetLoginErrorTimes(id: String): Boolean {
        val user = AuthUser {
            this.id = id
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的登录错误次数。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的登录错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun incrementSecurityPasswordErrorTimes(id: String): Boolean {
        val existingUser = dao.get(id) ?: return false
        val currentErrorTimes = existingUser.securityPasswordErrorTimes ?: 0
        val user = AuthUser {
            this.id = id
            this.securityPasswordErrorTimes = currentErrorTimes + 1
        }
        val success = dao.update(user)
        if (success) {
            log.debug("增加id为${id}的用户的安全密码错误次数。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("增加id为${id}的用户的安全密码错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun resetSecurityPasswordErrorTimes(id: String): Boolean {
        val user = AuthUser {
            this.id = id
            this.securityPasswordErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的安全密码错误次数。")
            userByIdCacheHandler.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的安全密码错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的用户。")
        userByIdCacheHandler.syncOnInsert(id)
        userIdByTenantIdAndUsernameCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthUser::id.name) as String
        if (success) {
            log.debug("更新id为${id}的用户。")
            userByIdCacheHandler.syncOnUpdate(id)
            userIdByTenantIdAndUsernameCacheHandler.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的用户失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val user = dao.get(id)
        if (user == null) {
            log.warn("删除id为${id}的用户时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的用户。")
            userByIdCacheHandler.syncOnDelete(id)
            userIdByTenantIdAndUsernameCacheHandler.syncOnDelete(user, id)
        } else {
            log.error("删除id为${id}的用户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val users = dao.inSearchById(ids)
        val tenantAndUsernames = users.mapNotNull { user ->
            user.id?.let { Pair(user.tenantId, user.username) }
        }
        val count = super.batchDelete(ids)
        log.debug("批量删除用户，期望删除${ids.size}条，实际删除${count}条。")
        userByIdCacheHandler.syncOnBatchDelete(ids)
        userIdByTenantIdAndUsernameCacheHandler.syncOnBatchDelete(ids, tenantAndUsernames)
        return count
    }

    //endregion your codes 2

}
