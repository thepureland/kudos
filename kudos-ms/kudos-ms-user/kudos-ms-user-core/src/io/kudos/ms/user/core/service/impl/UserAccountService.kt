package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountRecord
import io.kudos.ms.user.common.vo.user.UserAccountSearchPayload
import io.kudos.ms.user.core.cache.OrgByIdCache
import io.kudos.ms.user.core.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.cache.UserByIdCache
import io.kudos.ms.user.core.cache.UserIdByTenantIdAndUsernameCache
import io.kudos.ms.user.core.dao.UserAccountDao
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.ms.user.core.service.iservice.IUserAccountService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.security.CryptoKit
import io.kudos.ms.user.core.cache.UserOrgHashCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 用户账号业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class UserAccountService : BaseCrudService<String, UserAccount, UserAccountDao>(), IUserAccountService {
//endregion your codes 1

    //region your codes 2



    @Autowired
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Autowired
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    @Autowired
    private lateinit var orgByIdCache: OrgByIdCache

    @Autowired
    private lateinit var userByIdCache: UserByIdCache

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCache: UserIdByTenantIdAndUsernameCache


    private val log = LogFactory.getLog(this)

    override fun getUserOrgIds(userId: String): List<String> {
        return orgIdsByUserIdCache.getOrgIds(userId)
    }


    override fun getUserIds(tenantId: String): List<String> {
        val criteria = Criteria(UserAccount::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(UserAccount::active.name, OperatorEnum.EQ, true)
        val users = search(criteria)
        return users.mapNotNull { it.id }
    }


    override fun getUserOrgs(userId: String): List<UserOrgCacheItem> {
        val orgIds = getUserOrgIds(userId)
        if (orgIds.isEmpty()) {
            return emptyList()
        }
        val orgsMap = orgByIdCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        val orgIds = getUserOrgIds(userId)
        return orgIds.contains(orgId)
    }

    override fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem? {
        val userId = userIdByTenantIdAndUsernameCache.getUserId(tenantId, username)
        return userId?.let { userByIdCache.getUserById(it) }
    }

    override fun getUserRecord(id: String): UserAccountRecord? {
        val user = dao.get(id) ?: return null
        val record = UserAccountRecord()
        BeanKit.copyProperties(user, record)
        return record
    }

    override fun getUsersByTenantId(tenantId: String): List<UserAccountRecord> {
        val searchPayload = UserAccountSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<UserAccountRecord>
    }

    override fun getUsersByOrgId(orgId: String): List<UserAccountRecord> {
        val searchPayload = UserAccountSearchPayload().apply {
            this.orgId = orgId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<UserAccountRecord>
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val user = UserAccount {
            this.id = id
            this.active = active
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的启用状态为${active}。")
            userByIdCache.syncOnUpdate(id)
            val existingUser = dao.get(id)
            if (existingUser != null) {
                userIdByTenantIdAndUsernameCache.syncOnUpdateActive(id, active)
            }
        } else {
            log.error("更新id为${id}的用户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = CryptoKit.aesEncrypt(newPassword)
        val user = UserAccount {
            this.id = id
            this.loginPassword = encryptedPassword
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的登录密码。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的登录密码失败！")
        }
        return success
    }

    @Transactional
    override fun resetSecurityPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = CryptoKit.aesEncrypt(newPassword)
        val user = UserAccount {
            this.id = id
            this.securityPassword = encryptedPassword
            this.securityPasswordErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的安全密码。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的安全密码失败！")
        }
        return success
    }

    @Transactional
    override fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean {
        val user = UserAccount {
            this.id = id
            this.lastLoginIp = loginIp
            this.lastLoginTime = loginTime
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的最后登录信息。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的用户的最后登录信息失败！")
        }
        return success
    }

    @Transactional
    override fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean {
        val user = UserAccount {
            this.id = id
            this.lastLogoutTime = logoutTime
        }
        val success = dao.update(user)
        if (success) {
            log.debug("更新id为${id}的用户的最后登出信息。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的用户的最后登出信息失败！")
        }
        return success
    }

    @Transactional
    override fun incrementLoginErrorTimes(id: String): Boolean {
        val existingUser = dao.get(id) ?: return false
        val currentErrorTimes = existingUser.loginErrorTimes ?: 0
        val user = UserAccount {
            this.id = id
            this.loginErrorTimes = currentErrorTimes + 1
        }
        val success = dao.update(user)
        if (success) {
            log.debug("增加id为${id}的用户的登录错误次数。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("增加id为${id}的用户的登录错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun resetLoginErrorTimes(id: String): Boolean {
        val user = UserAccount {
            this.id = id
            this.loginErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的登录错误次数。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的登录错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun incrementSecurityPasswordErrorTimes(id: String): Boolean {
        val existingUser = dao.get(id) ?: return false
        val currentErrorTimes = existingUser.securityPasswordErrorTimes ?: 0
        val user = UserAccount {
            this.id = id
            this.securityPasswordErrorTimes = currentErrorTimes + 1
        }
        val success = dao.update(user)
        if (success) {
            log.debug("增加id为${id}的用户的安全密码错误次数。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("增加id为${id}的用户的安全密码错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun resetSecurityPasswordErrorTimes(id: String): Boolean {
        val user = UserAccount {
            this.id = id
            this.securityPasswordErrorTimes = 0
        }
        val success = dao.update(user)
        if (success) {
            log.debug("重置id为${id}的用户的安全密码错误次数。")
            userByIdCache.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的安全密码错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的用户。")
        userByIdCache.syncOnInsert(id)
        userIdByTenantIdAndUsernameCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, UserAccount::id.name) as String
        if (success) {
            log.debug("更新id为${id}的用户。")
            userByIdCache.syncOnUpdate(id)
            userIdByTenantIdAndUsernameCache.syncOnUpdate(any, id)
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
            userByIdCache.syncOnDelete(id)
            userIdByTenantIdAndUsernameCache.syncOnDelete(user, id)
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
        userByIdCache.syncOnBatchDelete(ids)
        userIdByTenantIdAndUsernameCache.syncOnBatchDelete(ids, tenantAndUsernames)
        return count
    }

    //endregion your codes 2

}
