package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.dao.UserAccountThirdDao
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingException
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditEvent
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditSnapshot
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdAuditService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime


/**
 * User account third-party binding service implementation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class UserAccountThirdService(
    dao: UserAccountThirdDao,
    private val userAccountDao: UserAccountDao,
    private val auditService: IUserAccountThirdAuditService,
) : BaseCrudService<String, UserAccountThird, UserAccountThirdDao>(dao), IUserAccountThirdService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getByUserAccountId(userId: String): List<UserAccountThird> =
        dao.searchByUserId(userId)

    @Transactional(readOnly = true)
    override fun getActiveByUserAccountId(userId: String): List<UserAccountThird> =
        dao.searchActiveByUserId(userId)

    override fun updateLastLoginTime(bindingId: String, lastLoginTime: LocalDateTime): Boolean =
        dao.updateProperties(bindingId, mapOf(UserAccountThird::lastLoginTime.name to lastLoginTime))

    override fun bindExternalIdentity(command: ExternalAccountBindingCommand): UserAccountThird =
        bindExternalIdentity(command, command.userId, BIND, null)

    override fun jitBindExternalIdentity(command: ExternalAccountBindingCommand): UserAccountThird =
        bindExternalIdentity(command, command.userId, JIT_BIND, null)

    override fun prebindExternalIdentity(command: AdminExternalAccountBindingCommand): UserAccountThird {
        require(command.actorUserId.isNotBlank()) { "actorUserId must not be blank" }
        require(command.operationReason.isNotBlank()) { "operationReason must not be blank" }
        return bindExternalIdentity(
            ExternalAccountBindingCommand(
                userId = command.userId,
                tenantId = command.tenantId,
                identityProviderId = command.identityProviderId,
                providerCode = command.providerCode,
                issuer = command.issuer,
                subject = command.subject,
                unionId = command.unionId,
                displayName = command.displayName,
                email = command.email,
                avatarUrl = command.avatarUrl,
            ),
            command.actorUserId,
            ADMIN_BIND,
            command.operationReason,
        )
    }

    private fun bindExternalIdentity(
        command: ExternalAccountBindingCommand,
        actorUserId: String,
        action: String,
        operationReason: String?,
    ): UserAccountThird {
        validate(command)
        userAccountDao.lockAccount(command.userId)
        val account = userAccountDao.get(command.userId)
        if (account == null || account.active != true) {
            deny(command, action, "ACCOUNT_UNAVAILABLE", actorUserId = actorUserId, operationReason = operationReason)
        }
        if (account.tenantId != command.tenantId) {
            deny(command, action, "ACCOUNT_TENANT_MISMATCH", actorUserId = actorUserId, operationReason = operationReason)
        }

        val identityBinding = dao.fetchByIdentityProviderSubject(
            command.tenantId,
            command.identityProviderId,
            command.issuer,
            command.subject,
        )
        if (identityBinding != null && identityBinding.userId != command.userId) {
            deny(
                command,
                action,
                "EXTERNAL_IDENTITY_ALREADY_BOUND",
                identityBinding.id,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = identityBinding.toAuditSnapshot(),
            )
        }
        val providerSlot = dao.searchByUserId(command.userId).firstOrNull {
            it.identityProviderId == command.identityProviderId ||
                (it.identityProviderId == null && it.accountProviderDictCode == command.providerCode)
        }
        if (providerSlot != null && providerSlot.active == true &&
            (providerSlot.accountProviderIssuer != command.issuer || providerSlot.subject != command.subject)
        ) {
            deny(
                command,
                action,
                "USER_PROVIDER_ALREADY_BOUND",
                providerSlot.id,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = providerSlot.toAuditSnapshot(),
            )
        }

        val isNew = identityBinding == null && providerSlot == null
        val binding = identityBinding ?: providerSlot ?: UserAccountThird {
            userId = command.userId
            tenantId = command.tenantId
            builtIn = false
            createTime = LocalDateTime.now()
        }
        val beforeSnapshot = if (isNew) null else binding.toAuditSnapshot()
        applyVerifiedClaims(binding, command)
        binding.active = true
        binding.updateTime = LocalDateTime.now()

        try {
            if (isNew) {
                binding.id = dao.insert(binding)
            } else {
                check(dao.update(binding)) { "External identity binding update affected no row" }
            }
        } catch (e: Exception) {
            if (e.hasIntegrityConstraintViolation()) {
                deny(
                    command,
                    action,
                    "EXTERNAL_IDENTITY_ALREADY_BOUND",
                    if (isNew) null else binding.id,
                    e,
                    actorUserId,
                    operationReason,
                    beforeSnapshot,
                )
            }
            throw e
        }
        audit(
            UserAccountThirdAuditEvent(
                bindingId = binding.id,
                userId = command.userId,
                tenantId = command.tenantId,
                identityProviderId = command.identityProviderId,
                providerCode = command.providerCode,
                subject = command.subject,
                action = action,
                success = true,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = beforeSnapshot,
                afterSnapshot = binding.toAuditSnapshot(),
            )
        )
        return binding
    }

    override fun unbindExternalIdentity(bindingId: String, userId: String, tenantId: String): Boolean =
        unbindExternalIdentity(bindingId, userId, tenantId, userId, UNBIND, null)

    override fun adminUnbindExternalIdentity(
        bindingId: String,
        tenantId: String,
        actorUserId: String,
        operationReason: String,
    ): Boolean {
        require(actorUserId.isNotBlank()) { "actorUserId must not be blank" }
        require(operationReason.isNotBlank()) { "operationReason must not be blank" }
        val binding = dao.get(bindingId) ?: throw ExternalAccountBindingException("EXTERNAL_BINDING_NOT_FOUND")
        return unbindExternalIdentity(
            bindingId,
            binding.userId,
            tenantId,
            actorUserId,
            ADMIN_UNBIND,
            operationReason,
        )
    }

    private fun unbindExternalIdentity(
        bindingId: String,
        userId: String,
        tenantId: String,
        actorUserId: String,
        action: String,
        operationReason: String?,
    ): Boolean {
        require(bindingId.isNotBlank()) { "bindingId must not be blank" }
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(tenantId.isNotBlank()) { "tenantId must not be blank" }
        userAccountDao.lockAccount(userId)
        val account = userAccountDao.get(userId)
            ?: throw ExternalAccountBindingException("ACCOUNT_UNAVAILABLE")
        val binding = dao.get(bindingId)
        if (binding == null || binding.userId != userId) {
            throw ExternalAccountBindingException("EXTERNAL_BINDING_NOT_FOUND")
        }
        if (binding.tenantId != tenantId) {
            deny(
                binding,
                action,
                actorUserId,
                "EXTERNAL_BINDING_NOT_FOUND",
                operationReason,
                binding.toAuditSnapshot(),
            )
        }
        if (account.tenantId != tenantId) {
            deny(binding, action, actorUserId, "ACCOUNT_TENANT_MISMATCH", operationReason)
        }
        if (binding.active != true) return false

        val beforeSnapshot = binding.toAuditSnapshot()
        val hasPassword = account.loginPassword.isNotBlank()
        val hasAnotherExternalIdentity = dao.searchActiveByUserId(userId).any { it.id != binding.id }
        if (!hasPassword && !hasAnotherExternalIdentity) {
            deny(
                binding,
                action,
                actorUserId,
                "LAST_AUTHENTICATION_METHOD",
                operationReason,
                beforeSnapshot,
            )
        }
        binding.active = false
        binding.updateTime = LocalDateTime.now()
        check(dao.updateOnly(binding, UserAccountThird::active.name, UserAccountThird::updateTime.name)) {
            "External identity binding update affected no row"
        }
        audit(
            UserAccountThirdAuditEvent(
                bindingId = binding.id,
                userId = binding.userId,
                tenantId = binding.tenantId,
                identityProviderId = binding.identityProviderId,
                providerCode = binding.accountProviderDictCode,
                subject = binding.subject,
                action = action,
                success = true,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = beforeSnapshot,
                afterSnapshot = binding.toAuditSnapshot(),
            )
        )
        return true
    }

    @Transactional(readOnly = true)
    override fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird? =
        dao.fetchByProviderSubject(tenantId, accountProviderDictCode, accountProviderIssuer, subject)

    @Transactional(readOnly = true)
    override fun getByIdentityProviderSubject(
        tenantId: String,
        identityProviderId: String,
        accountProviderIssuer: String?,
        subject: String,
    ): UserAccountThird? =
        dao.fetchByIdentityProviderSubject(tenantId, identityProviderId, accountProviderIssuer, subject)

    private fun validate(command: ExternalAccountBindingCommand) {
        require(command.userId.isNotBlank()) { "userId must not be blank" }
        require(command.tenantId.isNotBlank()) { "tenantId must not be blank" }
        require(command.identityProviderId.isNotBlank()) { "identityProviderId must not be blank" }
        require(command.providerCode.isNotBlank()) { "providerCode must not be blank" }
        require(command.subject.isNotBlank()) { "subject must not be blank" }
    }

    private fun applyVerifiedClaims(binding: UserAccountThird, command: ExternalAccountBindingCommand) {
        binding.userId = command.userId
        binding.tenantId = command.tenantId
        binding.identityProviderId = command.identityProviderId
        binding.accountProviderDictCode = command.providerCode.take(32)
        binding.accountProviderIssuer = command.issuer?.take(512)
        binding.subject = command.subject.take(255)
        binding.unionId = command.unionId?.take(255)
        binding.externalDisplayName = command.displayName?.take(128)
        binding.externalEmail = command.email?.take(254)
        binding.avatarUrl = command.avatarUrl?.take(512)
    }

    private fun deny(
        command: ExternalAccountBindingCommand,
        action: String,
        reason: String,
        bindingId: String? = null,
        cause: Throwable? = null,
        actorUserId: String = command.userId,
        operationReason: String? = null,
        beforeSnapshot: UserAccountThirdAuditSnapshot? = null,
    ): Nothing {
        audit(
            UserAccountThirdAuditEvent(
                bindingId = bindingId,
                userId = command.userId,
                tenantId = command.tenantId,
                identityProviderId = command.identityProviderId,
                providerCode = command.providerCode,
                subject = command.subject,
                action = action,
                success = false,
                reason = reason,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = beforeSnapshot,
            )
        )
        throw ExternalAccountBindingException(reason, cause)
    }

    private fun deny(
        binding: UserAccountThird,
        action: String,
        actorUserId: String,
        reason: String,
        operationReason: String? = null,
        beforeSnapshot: UserAccountThirdAuditSnapshot? = null,
    ): Nothing {
        audit(
            UserAccountThirdAuditEvent(
                bindingId = binding.id,
                userId = binding.userId,
                tenantId = binding.tenantId,
                identityProviderId = binding.identityProviderId,
                providerCode = binding.accountProviderDictCode,
                subject = binding.subject,
                action = action,
                success = false,
                reason = reason,
                actorUserId = actorUserId,
                operationReason = operationReason,
                beforeSnapshot = beforeSnapshot,
            )
        )
        throw ExternalAccountBindingException(reason)
    }

    private fun audit(event: UserAccountThirdAuditEvent) {
        runCatching { auditService.record(event) }
            .onFailure { log.error(it, "Failed to persist external identity binding audit") }
    }

    private fun Throwable.hasIntegrityConstraintViolation(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SQLIntegrityConstraintViolationException ||
                current.javaClass.simpleName.contains("DuplicateKey") ||
                current.javaClass.simpleName.contains("ConstraintViolation")
            ) return true
            current = current.cause
        }
        return false
    }

    private fun UserAccountThird.toAuditSnapshot() = UserAccountThirdAuditSnapshot(
        identityProviderId = identityProviderId,
        providerCode = accountProviderDictCode,
        issuer = accountProviderIssuer,
        subject = subject,
        active = active == true,
    )

    private companion object {
        const val BIND = "BIND"
        const val JIT_BIND = "JIT_BIND"
        const val UNBIND = "UNBIND"
        const val ADMIN_BIND = "ADMIN_BIND"
        const val ADMIN_UNBIND = "ADMIN_UNBIND"
    }


}
