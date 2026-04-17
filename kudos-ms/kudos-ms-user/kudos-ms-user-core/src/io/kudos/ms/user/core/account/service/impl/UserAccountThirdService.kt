package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.account.dao.UserAccountThirdDao
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 用户账号第三方绑定业务
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


    override fun getByUserAccountId(userId: String): List<UserAccountThird> {
        return dao.searchByUserId(userId)
    }

    override fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird? {
        return dao.fetchByProviderSubject(tenantId, accountProviderDictCode, accountProviderIssuer, subject)
    }


}