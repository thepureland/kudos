package io.kudos.ms.user.core.account.service.iservice

import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import java.time.LocalDateTime


/**
 * User account third-party binding service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUserAccountThirdService : IBaseReadOnlyService<String, UserAccountThird> {


    /**
     * Query the binding list for a user account.
     *
     * @param userId user account id
     * @return binding list
     */
    fun getByUserAccountId(userId: String): List<UserAccountThird>

    fun getActiveByUserAccountId(userId: String): List<UserAccountThird>

    fun bindExternalIdentity(command: ExternalAccountBindingCommand): UserAccountThird

    /** First binding created as part of an atomic JIT account provision. */
    fun jitBindExternalIdentity(command: ExternalAccountBindingCommand): UserAccountThird

    fun unbindExternalIdentity(bindingId: String, userId: String, tenantId: String): Boolean

    fun prebindExternalIdentity(command: AdminExternalAccountBindingCommand): UserAccountThird

    fun adminUnbindExternalIdentity(
        bindingId: String,
        tenantId: String,
        actorUserId: String,
        operationReason: String,
    ): Boolean

    fun updateLastLoginTime(bindingId: String, lastLoginTime: LocalDateTime): Boolean

    /**
     * Look up a binding record by third-party identity.
     *
     * @param tenantId tenant id
     * @param accountProviderDictCode third-party provider code
     * @param accountProviderIssuer issuer / provider tenant
     * @param subject third-party unique user identifier
     * @return binding record, or null if not found
     */
    fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird?

    fun getByIdentityProviderSubject(
        tenantId: String,
        identityProviderId: String,
        accountProviderIssuer: String?,
        subject: String,
    ): UserAccountThird?


}
