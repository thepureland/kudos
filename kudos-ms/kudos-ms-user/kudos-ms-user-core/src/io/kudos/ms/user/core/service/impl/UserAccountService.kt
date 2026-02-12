package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.security.CryptoKit
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountRecord
import io.kudos.ms.user.common.vo.user.UserAccountSearchPayload
import io.kudos.ms.user.core.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.cache.UserAccountHashCache
import io.kudos.ms.user.core.cache.UserOrgHashCache
import io.kudos.ms.user.core.dao.UserAccountDao
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.ms.user.core.service.iservice.IUserAccountService
import jakarta.annotation.Resource
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



    @Resource
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    private val log = LogFactory.getLog(this)

    override fun getUserOrgIds(userId: String): List<String> {
        return orgIdsByUserIdCache.getOrgIds(userId)
    }


    override fun getUserIds(tenantId: String): List<String> {
        return dao.searchActiveUserIdsByTenantId(tenantId)
    }


    override fun getUserOrgs(userId: String): List<UserOrgCacheItem> {
        val orgIds = getUserOrgIds(userId)
        if (orgIds.isEmpty()) {
            return emptyList()
        }
        val orgsMap = userOrgHashCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        val orgIds = getUserOrgIds(userId)
        return orgIds.contains(orgId)
    }

    override fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem? {
        val userId = userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
        return userId?.let { userAccountHashCache.getUserById(it) }
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
        return dao.search(searchPayload, UserAccountRecord::class)
    }

    override fun getUsersByOrgId(orgId: String): List<UserAccountRecord> {
        val searchPayload = UserAccountSearchPayload().apply {
            this.orgId = orgId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, UserAccountRecord::class)
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
            userAccountHashCache.syncOnUpdate(id)
//            val existingUser = dao.get(id)
//            if (existingUser != null) {
//                userIdByTenantIdAndUsernameCache.syncOnUpdateActive(id, active)
//            }
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnUpdate(id)
        } else {
            log.error("重置id为${id}的用户的安全密码错误次数失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的用户。")
        userAccountHashCache.syncOnInsert(id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, UserAccount::id.name) as String
        if (success) {
            log.debug("更新id为${id}的用户。")
            userAccountHashCache.syncOnUpdate(id)
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
            userAccountHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的用户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除用户，期望删除${ids.size}条，实际删除${count}条。")
        userAccountHashCache.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}
