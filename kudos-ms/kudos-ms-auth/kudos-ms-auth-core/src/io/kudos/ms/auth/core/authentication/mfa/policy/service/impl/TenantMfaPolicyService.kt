package io.kudos.ms.auth.core.authentication.mfa.policy.service.impl

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.TenantMfaPolicyProperties
import io.kudos.ms.auth.core.authentication.mfa.policy.dao.AuthTenantMfaPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.model.po.AuthTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.IMfaEnrollmentQuery
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
open class TenantMfaPolicyService(
    private val dao: AuthTenantMfaPolicyDao,
    private val userAccountService: IUserAccountService,
    private val authRoleService: IAuthRoleService,
    private val totpEnrollmentService: ITotpEnrollmentService,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val mfaEnrollmentQuery: IMfaEnrollmentQuery? = null,
    private val properties: TenantMfaPolicyProperties = TenantMfaPolicyProperties(),
) : ITenantMfaPolicyService {

    @Transactional(readOnly = true)
    override fun getEffective(tenantId: String): EffectiveTenantMfaPolicy {
        requireTenantId(tenantId)
        return dao.get(tenantId)?.toEffective() ?: EffectiveTenantMfaPolicy(tenantId = tenantId)
    }

    override fun save(command: TenantMfaPolicySaveCommand): EffectiveTenantMfaPolicy {
        requireTenantId(command.tenantId)
        if (command.actorUserId.isBlank()) fail("MFA_POLICY_ACTOR_REQUIRED")
        val reason = command.operationReason.trim()
        if (reason.isBlank() || reason.length > 512) fail("MFA_POLICY_REASON_INVALID")
        if (command.gracePeriodDays !in 0..90) fail("MFA_POLICY_GRACE_PERIOD_INVALID")
        val mode = parseMode(command.mode)
        val methods = parseMethods(command.allowedMethods)
        if (methods.isEmpty()) fail("MFA_POLICY_METHOD_REQUIRED")
        val hasDeployableMandatoryMethod = MfaMethodEnum.TOTP in methods ||
            properties.allowWebAuthnOnly && MfaMethodEnum.WEBAUTHN in methods
        if (mode != MfaRequirementModeEnum.OPTIONAL && !hasDeployableMandatoryMethod) {
            fail("MFA_POLICY_NO_AVAILABLE_METHOD")
        }
        val accountTypes = normalizeCodes(command.requiredAccountTypeCodes, 64)
        val roleCodes = normalizeCodes(command.requiredRoleCodes, 64)
        if (mode == MfaRequirementModeEnum.CONDITIONAL && accountTypes.isEmpty() && roleCodes.isEmpty()) {
            fail("MFA_POLICY_CONDITION_REQUIRED")
        }

        val now = LocalDateTime.now(clock)
        val existing = dao.get(command.tenantId)
        val policy = existing ?: AuthTenantMfaPolicy {
            id = command.tenantId
            tenantId = command.tenantId
            createUserId = command.actorUserId
            createReason = reason
            createTime = now
        }
        policy.tenantId = command.tenantId
        policy.mode = mode.name
        policy.gracePeriodDays = command.gracePeriodDays
        policy.allowedMethods = methods.map { it.name }.sorted().joinToString(",")
        policy.recoveryCodesEnabled = command.recoveryCodesEnabled
        policy.requiredAccountTypeCodes = accountTypes.csvOrNull()
        policy.requiredRoleCodes = roleCodes.csvOrNull()
        policy.updateUserId = command.actorUserId
        policy.updateReason = reason
        policy.updateTime = now
        if (existing == null) dao.insert(policy) else if (!dao.update(policy)) fail("MFA_POLICY_UPDATE_FAILED")
        return policy.toEffective()
    }

    @Transactional(readOnly = true)
    override fun evaluate(tenantId: String, userId: String): MfaPolicyDecision {
        requireTenantId(tenantId)
        if (userId.isBlank()) fail("MFA_POLICY_USER_REQUIRED")
        val user = userAccountService.get(userId) ?: fail("MFA_POLICY_ACCOUNT_NOT_FOUND")
        if (user.tenantId != tenantId) fail("MFA_POLICY_ACCOUNT_TENANT_MISMATCH")
        val policy = getEffective(tenantId)
        val required = when (policy.mode) {
            MfaRequirementModeEnum.OPTIONAL -> false
            MfaRequirementModeEnum.REQUIRED -> true
            MfaRequirementModeEnum.CONDITIONAL ->
                user.accountTypeDictCode in policy.requiredAccountTypeCodes ||
                    authRoleService.getUserRoles(userId).any { it.code in policy.requiredRoleCodes }
        }
        val enrolledMethods = mfaEnrollmentQuery?.enrolledMethods(userId, tenantId)
            ?: setOfNotNull(MfaMethodEnum.TOTP.takeIf { totpEnrollmentService.isEnabled(userId, tenantId) })
        val enrolled = policy.allowedMethods.any { it in enrolledMethods }
        val enrollmentRequired = required && !enrolled
        val graceBase = listOfNotNull(user.createTime, policy.effectiveFrom).maxOrNull()
        val graceExpiresAt = if (enrollmentRequired) graceBase?.plusDays(policy.gracePeriodDays.toLong()) else null
        // Administrator-granted exemptions are deliberately not folded in here: this evaluates the tenant's
        // policy against the account, and an exemption is an override applied at the enforcement point.
        return MfaPolicyDecision(
            policy = policy,
            required = required,
            enrolled = enrolled,
            enrollmentRequired = enrollmentRequired,
            gracePeriodActive = graceExpiresAt?.isAfter(LocalDateTime.now(clock)) == true,
            graceExpiresAt = graceExpiresAt,
        )
    }

    private fun AuthTenantMfaPolicy.toEffective() = EffectiveTenantMfaPolicy(
        tenantId = tenantId,
        mode = parseMode(mode),
        gracePeriodDays = gracePeriodDays,
        allowedMethods = parseMethods(allowedMethods.split(',')),
        recoveryCodesEnabled = recoveryCodesEnabled,
        requiredAccountTypeCodes = requiredAccountTypeCodes.csvValues(),
        requiredRoleCodes = requiredRoleCodes.csvValues(),
        effectiveFrom = updateTime,
        configured = true,
    )

    private fun parseMode(raw: String): MfaRequirementModeEnum = runCatching {
        MfaRequirementModeEnum.valueOf(raw.trim().uppercase())
    }.getOrElse { fail("MFA_POLICY_MODE_INVALID", it) }

    private fun parseMethods(values: Collection<String>): Set<MfaMethodEnum> = values.map { raw ->
        runCatching { MfaMethodEnum.valueOf(raw.trim().uppercase()) }
            .getOrElse { fail("MFA_POLICY_METHOD_INVALID", it) }
    }.toSet()

    private fun normalizeCodes(values: Collection<String>, maxLength: Int): Set<String> = values.map { raw ->
        val value = raw.trim()
        if (value.isBlank() || value.length > maxLength || value.contains(',')) {
            fail("MFA_POLICY_CONDITION_CODE_INVALID")
        }
        value
    }.toSortedSet()

    private fun Set<String>.csvOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")
    private fun String?.csvValues(): Set<String> = this?.split(',')?.map { it.trim() }
        ?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()

    private fun requireTenantId(tenantId: String) {
        if (tenantId.isBlank() || tenantId.length > 36) fail("MFA_POLICY_TENANT_INVALID")
    }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw TenantMfaPolicyException(errorCode, cause)
}
