package io.kudos.ms.user.core.account.service.impl

import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingException
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningException
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import java.util.Locale

/** Transaction boundary for JIT account creation plus its mandatory first identity binding. */
@Service
open class ExternalAccountProvisioningService(
    private val userAccountService: IUserAccountService,
    private val userAccountThirdService: IUserAccountThirdService,
    private val userOrgUserService: IUserOrgUserService,
    private val userContactWayService: IUserContactWayService,
) : IExternalAccountProvisioningService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun provision(command: ExternalAccountProvisioningCommand): UserAccountThird {
        validate(command)
        val existing = userAccountThirdService.getByIdentityProviderSubject(
            command.tenantId,
            command.identityProviderId,
            command.issuer,
            command.subject,
        )
        if (existing != null) {
            if (existing.active == true) return existing
            throw ExternalAccountProvisioningException("EXTERNAL_IDENTITY_DISABLED")
        }

        val now = LocalDateTime.now()
        val user = UserAccount {
            username = command.username.take(MAX_USERNAME_LENGTH)
            tenantId = command.tenantId
            // Blank means this account has no local-password authentication method yet.
            loginPassword = ""
            supervisorId = command.defaultSupervisorId ?: ROOT_SUPERVISOR_ID
            accountTypeDictCode = command.accountTypeDictCode?.take(5)
            accountStatusDictCode = command.accountStatusDictCode?.take(5)
            defaultLocale = command.defaultLocale?.take(5)
            defaultTimezone = command.defaultTimezone?.take(64)
            defaultCurrency = command.defaultCurrency?.take(3)
            orgId = command.defaultOrgId?.take(128)
            loginErrorTimes = 0
            securityPasswordErrorTimes = 0
            active = true
            builtIn = false
            createUserName = JIT_ACTOR_NAME
            createTime = now
            updateTime = now
            remark = "JIT provisioned by external provider ${command.providerCode.take(32)}"
        }

        try {
            user.id = userAccountService.insert(user)
            command.email
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.takeIf { command.emailVerified && it.isNotBlank() }
                ?.let { verifiedEmail ->
                    userContactWayService.insert(UserContactWay {
                        userId = user.id
                        contactWayDictCode = EMAIL_CONTACT_WAY_CODE
                        contactWayValue = verifiedEmail.take(MAX_EMAIL_LENGTH)
                        contactWayStatusDictCode = VERIFIED_CONTACT_WAY_STATUS
                        priority = 0
                        active = true
                        builtIn = false
                        createUserName = JIT_ACTOR_NAME
                        createTime = now
                        remark = "Verified by ${command.providerCode.take(32)}"
                    })
                }
            command.defaultOrgId?.takeIf { it.isNotBlank() }?.let { orgId ->
                userOrgUserService.batchBind(orgId, listOf(user.id))
            }
            return userAccountThirdService.jitBindExternalIdentity(
                ExternalAccountBindingCommand(
                    userId = user.id,
                    tenantId = command.tenantId,
                    identityProviderId = command.identityProviderId,
                    providerCode = command.providerCode,
                    issuer = command.issuer,
                    subject = command.subject,
                    unionId = command.unionId,
                    displayName = command.displayName,
                    email = command.email,
                    avatarUrl = command.avatarUrl,
                )
            )
        } catch (e: ExternalAccountBindingException) {
            throw ExternalAccountProvisioningException(e.errorCode, e)
        } catch (e: Exception) {
            if (e.hasIntegrityConstraintViolation()) {
                throw ExternalAccountProvisioningException("EXTERNAL_JIT_PROVISIONING_CONFLICT", e)
            }
            throw e
        }
    }

    private fun validate(command: ExternalAccountProvisioningCommand) {
        require(command.username.isNotBlank()) { "username must not be blank" }
        require(command.tenantId.isNotBlank()) { "tenantId must not be blank" }
        require(command.identityProviderId.isNotBlank()) { "identityProviderId must not be blank" }
        require(command.providerCode.isNotBlank()) { "providerCode must not be blank" }
        require(command.subject.isNotBlank()) { "subject must not be blank" }
    }

    private fun Throwable.hasIntegrityConstraintViolation(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SQLIntegrityConstraintViolationException ||
                current is DataIntegrityViolationException ||
                current.javaClass.simpleName.contains("DuplicateKey") ||
                current.javaClass.simpleName.contains("ConstraintViolation")
            ) return true
            current = current.cause
        }
        return false
    }

    private companion object {
        const val MAX_USERNAME_LENGTH = 32
        const val MAX_EMAIL_LENGTH = 254
        const val ROOT_SUPERVISOR_ID = "00000000-0000-0000-0000-000000000000"
        const val JIT_ACTOR_NAME = "external-jit"
        const val EMAIL_CONTACT_WAY_CODE = "201"
        const val VERIFIED_CONTACT_WAY_STATUS = "10"
    }
}
