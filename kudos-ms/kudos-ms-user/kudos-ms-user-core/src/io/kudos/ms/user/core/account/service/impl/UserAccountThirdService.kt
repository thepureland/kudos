package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.account.dao.UserAccountThirdDao
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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
    dao: UserAccountThirdDao
) : BaseCrudService<String, UserAccountThird, UserAccountThirdDao>(dao), IUserAccountThirdService {


    @Transactional(readOnly = true)
    override fun getByUserAccountId(userId: String): List<UserAccountThird> =
        dao.searchByUserId(userId)

    @Transactional(readOnly = true)
    override fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird? =
        dao.fetchByProviderSubject(tenantId, accountProviderDictCode, accountProviderIssuer, subject)


}