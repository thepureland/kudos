package io.kudos.ms.user.core.account.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.core.account.model.po.UserAccountThird


/**
 * User account third-party binding service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUserAccountThirdService : IBaseCrudService<String, UserAccountThird> {


    /**
     * Query the binding list for a user account.
     *
     * @param userId user account id
     * @return binding list
     */
    fun getByUserAccountId(userId: String): List<UserAccountThird>

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


}