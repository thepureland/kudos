package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.lt
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.ability.security.common.support.PasswordEncodingKit
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
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.IPasswordPolicy
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPolicyException
import io.kudos.ms.user.core.account.security.PasswordReusedException
import io.kudos.ms.user.core.account.security.PasswordPurpose
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
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
    private val passwordPolicy: IPasswordPolicy,
    private val passwordHistories: List<IPasswordHistory>,
    private val passwordEncoder: PasswordEncoder,
    private val credentialStores: List<IAccountCredentialStore> = emptyList(),
) : BaseCrudService<String, UserAccount, UserAccountDao>(dao), IUserAccountService {

    /**
     * The credential owner, when one is deployed alongside this module.
     *
     * A list rather than a nullable bean because that is how [IPasswordHistory] is already wired here, and
     * because an absent optional collaborator then costs nothing at construction. At most one is expected.
     */
    private val credentialStore: IAccountCredentialStore?
        get() = credentialStores.firstOrNull()




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
    override fun updateActive(id: String, active: Boolean): Boolean {
        val tenantId = if (active) null else dao.get(id)?.tenantId
        val success = updateAndPublish(id, "Updated active flag of user id=${id} to ${active}") {
            this.active = active
        }
        if (success && tenantId != null) {
            publishAuthenticationInvalidated(
                id,
                tenantId,
                UserAuthenticationInvalidated.Reason.ACCOUNT_DISABLED,
            )
        }
        return success
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String): Boolean {
        val existing = dao.get(id) ?: return false
        val context = PasswordPolicyContext(PasswordPurpose.LOGIN, id, existing.username, existing.tenantId)
        val encryptedPassword = protectPassword(newPassword, context, existing.loginPassword)
        val success = updateAndPublish(id, "Reset login password of user id=${id}") {
            this.loginPassword = columnValueFor(encryptedPassword)
            this.loginErrorTimes = 0
        }
        if (success) {
            // Only once the account row actually took the change, so a vanished account does not leave a
            // rotated credential behind.
            credentialStore?.storePassword(encryptedPassword, context)
            recordPasswordHistory(
                existing.loginPassword,
                PasswordPolicyContext(PasswordPurpose.LOGIN, id, existing.username, existing.tenantId),
            )
            publishAuthenticationInvalidated(
                id,
                existing.tenantId,
                UserAuthenticationInvalidated.Reason.LOGIN_PASSWORD_CHANGED,
            )
        }
        return success
    }

    @Transactional
    override fun resetSecurityPassword(id: String, newPassword: String): Boolean {
        val existing = dao.get(id) ?: return false
        val encryptedPassword = protectPassword(
            newPassword,
            PasswordPolicyContext(PasswordPurpose.SECURITY, id, existing.username, existing.tenantId),
            existing.securityPassword,
        )
        val success = updateAndPublish(id, "Reset security password of user id=${id}") {
            this.securityPassword = encryptedPassword
            this.securityPasswordErrorTimes = 0
        }
        if (success) {
            recordPasswordHistory(
                existing.securityPassword,
                PasswordPolicyContext(PasswordPurpose.SECURITY, id, existing.username, existing.tenantId),
            )
            publishAuthenticationInvalidated(
                id,
                existing.tenantId,
                UserAuthenticationInvalidated.Reason.SECURITY_PASSWORD_CHANGED,
            )
        }
        return success
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
        val account = mutableAccount(any)
        val loginPassword = stringProperty(any, UserAccount::loginPassword.name)
        val encodedLoginPassword = loginPassword
            ?.takeIf(String::isNotBlank)
            ?.let {
                protectPassword(
                    it,
                    PasswordPolicyContext(
                        purpose = PasswordPurpose.LOGIN,
                        username = stringProperty(any, UserAccount::username.name),
                        tenantId = stringProperty(any, UserAccount::tenantId.name),
                    ),
                    allowExistingHash = any is UserAccount,
                )
            }
        // Enrolment waits for the row: the credential is keyed by the account id, which only exists after
        // the insert. The column keeps the hash only when nothing else owns it.
        account.loginPassword = if (credentialStore == null) encodedLoginPassword.orEmpty() else ""
        val securityPassword = stringProperty(any, UserAccount::securityPassword.name)
        account.securityPassword = securityPassword
            ?.takeIf(String::isNotBlank)
            ?.let {
                protectPassword(
                    it,
                    PasswordPolicyContext(
                        purpose = PasswordPurpose.SECURITY,
                        username = stringProperty(any, UserAccount::username.name),
                        tenantId = stringProperty(any, UserAccount::tenantId.name),
                    ),
                    allowExistingHash = any is UserAccount,
                )
            }
        val id = dao.insert(account)
        if (encodedLoginPassword != null) {
            credentialStore?.storePassword(
                encodedLoginPassword,
                PasswordPolicyContext(PasswordPurpose.LOGIN, id, account.username, account.tenantId),
            )
        }
        log.debug("Inserted user id=${id}.")
        eventPublisher.publishEvent(UserAccountInserted(id = id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = BeanKit.getProperty(any, UserAccount::id.name) as String
        val existing = dao.get(id) ?: return false
        val requestedTenantId = stringProperty(any, UserAccount::tenantId.name)
        require(requestedTenantId.isNullOrBlank() || requestedTenantId == existing.tenantId) {
            "A user account cannot be moved to another tenant"
        }
        val account = mutableAccount(any)
        val requestedUsername = stringProperty(any, UserAccount::username.name) ?: existing.username
        val requestedLoginPassword = stringProperty(any, UserAccount::loginPassword.name)
        val loginContext =
            PasswordPolicyContext(PasswordPurpose.LOGIN, id, requestedUsername, existing.tenantId)
        val encodedLoginPassword = requestedLoginPassword
            ?.takeIf(String::isNotBlank)
            ?.let {
                protectPassword(
                    it,
                    loginContext,
                    existing.loginPassword,
                    allowExistingHash = any is UserAccount,
                )
            }
        // Comparing hashes stops working once the column is empty on both sides, so the fact that a new
        // password was supplied and survived the reuse check is what marks the change from here on.
        val loginPasswordChanged = encodedLoginPassword != null &&
            (credentialStore != null || encodedLoginPassword != existing.loginPassword)
        val protectedLoginPassword =
            encodedLoginPassword?.let { columnValueFor(it) } ?: existing.loginPassword
        val requestedSecurityPassword = stringProperty(any, UserAccount::securityPassword.name)
        val protectedSecurityPassword = requestedSecurityPassword
            ?.takeIf(String::isNotBlank)
            ?.let {
                protectPassword(
                    it,
                    PasswordPolicyContext(PasswordPurpose.SECURITY, id, requestedUsername, existing.tenantId),
                    existing.securityPassword,
                    allowExistingHash = any is UserAccount,
                )
            }
            ?: existing.securityPassword
        account.tenantId = existing.tenantId
        account.loginPassword = protectedLoginPassword
        account.securityPassword = protectedSecurityPassword
        // These credentials have dedicated maintenance endpoints and are never writable through generic CRUD.
        account.authenticationKey = existing.authenticationKey
        account.sessionKey = existing.sessionKey
        val success = dao.update(account)
        if (success) {
            log.debug("Updated user id=${id}.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
            if (loginPasswordChanged) {
                // After the row took the change, so a failed update does not leave a rotated credential.
                encodedLoginPassword?.let { credentialStore?.storePassword(it, loginContext) }
                // A no-op when a credential store owns the password: it filed the outgoing hash itself,
                // being the only side that still has it.
                recordPasswordHistory(existing.loginPassword, loginContext)
            }
            if (protectedSecurityPassword != existing.securityPassword) {
                recordPasswordHistory(
                    existing.securityPassword,
                    PasswordPolicyContext(PasswordPurpose.SECURITY, id, requestedUsername, existing.tenantId),
                )
            }
            val reason = when {
                loginPasswordChanged ->
                    UserAuthenticationInvalidated.Reason.LOGIN_PASSWORD_CHANGED
                protectedSecurityPassword != existing.securityPassword ->
                    UserAuthenticationInvalidated.Reason.SECURITY_PASSWORD_CHANGED
                else -> null
            }
            if (reason != null) publishAuthenticationInvalidated(id, existing.tenantId, reason)
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
        if (!persistAuthKey(id, secret)) {
            log.error("Failed to reset TOTP secret (user missing?): userId=${id}")
            return null
        }
        // Standard otpauth URL; the front end can render it directly as a QR code (zxing/qrcode.js etc.).
        val otpauthUrl = "otpauth://totp/${encodeOtpAuthLabel(issuer, accountName)}" +
            "?secret=${secret}&issuer=${java.net.URLEncoder.encode(issuer, Charsets.UTF_8)}"
        log.debug("Reset TOTP secret for user id=${id}.")
        return AuthKeySetup(secret = secret, otpauthUrl = otpauthUrl)
    }

    @Transactional
    override fun activateVerifiedAuthKey(id: String, secret: String): Boolean {
        val normalized = secret.trim().uppercase()
        require(TOTP_SECRET_PATTERN.matches(normalized)) { "Invalid TOTP secret format" }
        val tenantId = dao.get(id)?.tenantId ?: return false
        val success = dao.activateAuthenticationKeyIfAbsent(id, normalized)
        if (success) {
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
            publishAuthenticationInvalidated(
                id,
                tenantId,
                UserAuthenticationInvalidated.Reason.AUTHENTICATOR_CHANGED,
            )
            log.debug("Activated verified TOTP secret for user id=${id}.")
        }
        return success
    }

    private fun persistAuthKey(id: String, secret: String): Boolean {
        val tenantId = dao.get(id)?.tenantId ?: return false
        val user = UserAccount {
            this.id = id
            this.authenticationKey = secret
        }
        if (!dao.update(user)) return false
        eventPublisher.publishEvent(UserAccountUpdated(id = id))
        publishAuthenticationInvalidated(
            id,
            tenantId,
            UserAuthenticationInvalidated.Reason.AUTHENTICATOR_CHANGED,
        )
        return true
    }

    @Transactional
    override fun cleanAuthKey(id: String): Boolean {
        val tenantId = dao.get(id)?.tenantId
        // For ktorm update, setting a column to null requires dao.updateProperties.
        val success = dao.updateProperties(id, mapOf(UserAccount::authenticationKey.name to null))
        if (success) {
            log.debug("Cleared TOTP secret for user id=${id}.")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
            if (tenantId != null) {
                publishAuthenticationInvalidated(
                    id,
                    tenantId,
                    UserAuthenticationInvalidated.Reason.AUTHENTICATOR_CHANGED,
                )
            }
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

        /** BCrypt compares at most 72 input bytes; reject longer values instead of accepting equivalent passwords. */
        private const val MAX_BCRYPT_PASSWORD_BYTES = 72
        private val TOTP_SECRET_PATTERN = Regex("^[A-Z2-7]{16,128}={0,6}$")

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
        val tenantId = dao.get(id)?.tenantId
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
            if (tenantId != null) {
                publishAuthenticationInvalidated(
                    id,
                    tenantId,
                    UserAuthenticationInvalidated.Reason.ACCOUNT_FROZEN,
                )
            }
        } else {
            log.warn("Failed to freeze account (user missing?): userId=${id}")
        }
        return success
    }

    @Transactional
    override fun upgradeLoginPasswordEncoding(
        id: String,
        expectedEncodedPassword: String,
        upgradedEncodedPassword: String,
    ): Boolean {
        require(PasswordEncodingKit.looksLikeEncodedPassword(expectedEncodedPassword)) {
            "Expected password must be encoded"
        }
        require(PasswordEncodingKit.looksLikeEncodedPassword(upgradedEncodedPassword)) {
            "Upgraded password must be encoded"
        }
        val success = dao.upgradeLoginPasswordEncoding(id, expectedEncodedPassword, upgradedEncodedPassword)
        if (success) {
            log.debug("Upgraded login-password encoding of user id=$id.")
            // This is the same credential with stronger metadata/parameters, not a password change:
            // evict caches, but do not add history or revoke sessions.
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        }
        return success
    }

    private fun mutableAccount(any: Any): UserAccount =
        if (any is UserAccount) any else UserAccount().also { BeanKit.copyProperties(any, it) }

    private fun stringProperty(any: Any, name: String): String? =
        runCatching { BeanKit.getProperty(any, name) as? String }.getOrNull()

    private fun protectPassword(
        password: String,
        context: PasswordPolicyContext,
        currentHash: String? = null,
        allowExistingHash: Boolean = false,
    ): String {
        if (allowExistingHash && PasswordEncodingKit.looksLikeEncodedPassword(password)) return password
        require(password.toByteArray(Charsets.UTF_8).size <= MAX_BCRYPT_PASSWORD_BYTES) {
            "Password must not exceed $MAX_BCRYPT_PASSWORD_BYTES UTF-8 bytes"
        }
        val violations = passwordPolicy.violations(password, context)
        if (violations.isNotEmpty()) throw PasswordPolicyException(violations)
        if (isCurrentPassword(password, context, currentHash) ||
            passwordHistories.any { it.isReused(password, context) }
        ) {
            throw PasswordReusedException()
        }
        return requireNotNull(passwordEncoder.encode(password)) { "Password encoder returned null" }
    }

    /**
     * Whether [password] is the one already in force.
     *
     * With a credential store deployed, [currentHash] comes from a column that no longer holds anything, so
     * comparing against it would answer "not the same password" for every password — this check has to ask
     * the store or it silently stops rejecting anything.
     */
    private fun isCurrentPassword(
        password: String,
        context: PasswordPolicyContext,
        currentHash: String?,
    ): Boolean {
        val store = credentialStore
        if (store != null && context.purpose == PasswordPurpose.LOGIN &&
            !context.userId.isNullOrBlank() && !context.tenantId.isNullOrBlank()
        ) {
            return store.verifyPassword(password, context)
        }
        return matchesPassword(password, currentHash)
    }

    /**
     * What `user_account.login_password` should hold for a newly encoded password.
     *
     * Empty once a credential store is deployed: the column stays only so this module remains deployable on
     * its own, and leaving a live hash in a second place is exactly what this migration retires.
     */
    private fun columnValueFor(encodedPassword: String): String =
        if (credentialStore == null) encodedPassword else ""

    private fun recordPasswordHistory(encodedPassword: String?, context: PasswordPolicyContext) {
        if (!PasswordEncodingKit.looksLikeEncodedPassword(encodedPassword)) return
        val supportedEncoding = encodedPassword ?: return
        passwordHistories.forEach { it.record(supportedEncoding, context) }
    }

    private fun matchesPassword(password: String, encodedPassword: String?): Boolean =
        PasswordEncodingKit.matches(passwordEncoder, password, encodedPassword)

    private fun publishAuthenticationInvalidated(
        id: String,
        tenantId: String,
        reason: UserAuthenticationInvalidated.Reason,
    ) {
        eventPublisher.publishEvent(UserAuthenticationInvalidated(id, tenantId, reason))
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
