package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.lt
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.request.UserAccountQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAccountInserted
import io.kudos.ms.user.core.account.event.UserAccountUpdated
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * User account service implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserAccountService(
    dao: UserAccountDao,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, UserAccount, UserAccountDao>(dao), IUserAccountService {




    @Resource
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserOrgIds(userId: String): List<String> = orgIdsByUserIdCache.getOrgIds(userId)


    @Transactional(readOnly = true)
    override fun getUserIds(tenantId: String): List<String> = dao.searchActiveUserIdsByTenantId(tenantId)


    @Transactional(readOnly = true)
    override fun getUserOrgs(userId: String): List<UserOrgCacheEntry> {
        val orgIds = getUserOrgIds(userId)
        if (orgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun isUserInOrg(userId: String, orgId: String): Boolean = orgId in getUserOrgIds(userId)

    @Transactional(readOnly = true)
    override fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry? =
        userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
            ?.let { userAccountHashCache.getUserById(it) }

    @Transactional(readOnly = true)
    override fun getUserRecord(id: String): UserAccountRow? = dao.getAs<UserAccountRow>(id)

    @Transactional(readOnly = true)
    override fun getUsersByTenantId(tenantId: String): List<UserAccountRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(UserAccountQuery(tenantId = tenantId), UserAccountRow::class)

    @Transactional(readOnly = true)
    override fun getUsersByOrgId(orgId: String): List<UserAccountRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(UserAccountQuery(orgId = orgId), UserAccountRow::class)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean =
        updateAndPublish(id, "Updated active flag of user id=${id} to ${active}") {
            this.active = active
        }

    @Transactional
    override fun resetPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = PasswordKit.hash(newPassword)
        return updateAndPublish(id, "Reset login password of user id=${id}") {
            this.loginPassword = encryptedPassword
            this.loginErrorTimes = 0
        }
    }

    @Transactional
    override fun resetSecurityPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = PasswordKit.hash(newPassword)
        return updateAndPublish(id, "Reset security password of user id=${id}") {
            this.securityPassword = encryptedPassword
            this.securityPasswordErrorTimes = 0
        }
    }

    @Transactional
    override fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean =
        updateAndPublish(id, "Updated last-login info of user id=${id}") {
            this.lastLoginIp = loginIp
            this.lastLoginTime = loginTime
            this.loginErrorTimes = 0
        }

    @Transactional
    override fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean =
        updateAndPublish(id, "Updated last-logout info of user id=${id}") {
            this.lastLogoutTime = logoutTime
        }

    @Transactional
    override fun incrementLoginErrorTimes(id: String): Boolean {
        val existing = dao.get(id) ?: return false
        val current = existing.loginErrorTimes ?: 0
        return updateAndPublish(id, "Incremented login error count of user id=${id}") {
            this.loginErrorTimes = current + 1
        }
    }

    @Transactional
    override fun resetLoginErrorTimes(id: String): Boolean =
        updateAndPublish(id, "Reset login error count of user id=${id}") {
            this.loginErrorTimes = 0
        }

    @Transactional
    override fun incrementSecurityPasswordErrorTimes(id: String): Boolean {
        val existing = dao.get(id) ?: return false
        val current = existing.securityPasswordErrorTimes ?: 0
        return updateAndPublish(id, "Incremented security-password error count of user id=${id}") {
            this.securityPasswordErrorTimes = current + 1
        }
    }

    @Transactional
    override fun resetSecurityPasswordErrorTimes(id: String): Boolean =
        updateAndPublish(id, "Reset security-password error count of user id=${id}") {
            this.securityPasswordErrorTimes = 0
        }

    /**
     * Shared template: build a [UserAccount] containing only id + changed fields, call [UserAccountDao.update],
     * log debug + publish [UserAccountUpdated] on success, or log error on failure.
     *
     * Extracted to consolidate the "build -> update -> log + event" boilerplate that was previously
     * scattered across 9 update methods, avoiding missed events or drifting log wording when fields are added.
     */
    private inline fun updateAndPublish(id: String, actionDesc: String, build: UserAccount.() -> Unit): Boolean {
        val user = UserAccount { this.id = id }.apply(build)
        val success = dao.update(user)
        if (success) {
            log.debug("$actionDesc.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.error("${actionDesc} failed!")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("Inserted user id=${id}.")
        eventPublisher.publishEvent(UserAccountInserted(id = id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, UserAccount::id.name) as String
        if (success) {
            log.debug("Updated user id=${id}.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.error("Failed to update user id=${id}!")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val user = dao.get(id) ?: run {
            log.warn("Failed to delete user id=${id}: already does not exist!")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted user id=${id}.")
            eventPublisher.publishEvent(UserAccountDeleted(id, user.tenantId, user.username))
        } else {
            log.error("Failed to delete user id=${id}!")
        }
        return success
    }

    @Transactional
    override fun resetAuthKey(id: String, accountName: String, issuer: String): AuthKeySetup? {
        val secret = GoogleAuthenticator.generateSecretKey()
            ?: run {
                log.error("Failed to generate TOTP secret: userId=${id}")
                return null
            }
        val user = UserAccount {
            this.id = id
            this.authenticationKey = secret
        }
        if (!dao.update(user)) {
            log.error("Failed to reset TOTP secret (user missing?): userId=${id}")
            return null
        }
        // Standard otpauth URL; the front end can render it directly as a QR code (zxing/qrcode.js etc.).
        val otpauthUrl = "otpauth://totp/${encodeOtpAuthLabel(issuer, accountName)}" +
            "?secret=${secret}&issuer=${java.net.URLEncoder.encode(issuer, Charsets.UTF_8)}"
        log.debug("Reset TOTP secret for user id=${id}.")
        eventPublisher.publishEvent(UserAccountUpdated(id = id))
        return AuthKeySetup(secret = secret, otpauthUrl = otpauthUrl)
    }

    @Transactional
    override fun cleanAuthKey(id: String): Boolean {
        // For ktorm update, setting a column to null requires dao.updateProperties.
        val success = dao.updateProperties(id, mapOf(UserAccount::authenticationKey.name to null))
        if (success) {
            log.debug("Cleared TOTP secret for user id=${id}.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("Failed to clear TOTP secret (user missing?): userId=$id")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun verifyAuthCode(id: String, code: Long): Boolean {
        val key = dao.get(id)?.authenticationKey ?: return false
        return GoogleAuthenticator().checkCode(key, code, System.currentTimeMillis())
    }

    companion object {

        /**
         * Build the label segment of the `otpauth://` URI per RFC 6238 / Google Authenticator conventions.
         *
         * Format: URL-encode the whole `issuer:accountName` -- the colon becomes `%3A` after encoding,
         * which matches what the GA app expects. If issuer or accountName contains spaces / special characters
         * (common with non-ASCII usernames), failing to encode would corrupt the entire URI.
         *
         * Pure function and `internal` so the encoding can be unit-tested directly without a Spring context.
         *
         * @param issuer application identifier (usually the product name)
         * @param accountName account name (user login name / email)
         * @return the encoded label segment
         * @author K
         * @since 1.0.0
         */
        internal fun encodeOtpAuthLabel(issuer: String, accountName: String): String =
            java.net.URLEncoder.encode("$issuer:$accountName", Charsets.UTF_8)
    }

    @Transactional
    override fun freezeAccount(
        id: String,
        freezeType: String,
        freezeTitle: String?,
        freezeContent: String?,
        freezeStartTime: LocalDateTime?,
        freezeEndTime: LocalDateTime?,
    ): Boolean {
        require(freezeType.isNotBlank()) { "freezeType must not be blank" }
        // Use updateProperties to update explicitly (including nulls). ktorm's plain update is a no-op
        // for null fields, but here we must clear start/end when the caller does not pass them.
        val success = dao.updateProperties(
            id, mapOf(
                UserAccount::freezeType.name to freezeType,
                UserAccount::freezeTime.name to LocalDateTime.now(),
                UserAccount::freezeStartTime.name to freezeStartTime,
                UserAccount::freezeEndTime.name to freezeEndTime,
                UserAccount::freezeTitle.name to freezeTitle,
                UserAccount::freezeContent.name to freezeContent,
            )
        )
        if (success) {
            log.debug("Froze account id=${id}, type=${freezeType}")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("Failed to freeze account (user missing?): userId=${id}")
        }
        return success
    }

    @Transactional
    override fun unfreezeAccount(id: String): Boolean {
        // Clear all 6 columns. freezeTime is also cleared to avoid the misleading "was once frozen" residue.
        val success = dao.updateProperties(
            id, mapOf(
                UserAccount::freezeType.name to null,
                UserAccount::freezeTime.name to null,
                UserAccount::freezeStartTime.name to null,
                UserAccount::freezeEndTime.name to null,
                UserAccount::freezeTitle.name to null,
                UserAccount::freezeContent.name to null,
            )
        )
        if (success) {
            log.debug("Unfroze account id=${id}.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("Failed to unfreeze account (user missing?): userId=${id}")
        }
        return success
    }

    @Transactional
    override fun cleanExpiredFreezes(): Int {
        // freeze_end_time IS NOT NULL AND freeze_end_time < now()
        // The `lt` operator maps to SQL `<`, which naturally does not match NULL --
        // permanent freezes (freeze_end_time=null) are not cleared.
        val expired = dao.searchAs<UserAccount>(Criteria(UserAccount::freezeEndTime lt LocalDateTime.now()))
        val cleared = expired.count { unfreezeAccount(it.id) }
        if (cleared > 0) log.info("auto-unfreeze: cleaned $cleared expired freeze records in total")
        return cleared
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // Snapshot tenantId/username first; after AFTER_COMMIT, downstream (tenantId, username) caches
        // can no longer look them up.
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { UserAccountBatchDeleted.Item(it.id, it.tenantId, it.username) }
        val count = super.batchDelete(ids)
        log.debug("Batch deleted users: expected ${ids.size}, actually deleted ${count}.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(UserAccountBatchDeleted(snapshots))
        }
        return count
    }


}
