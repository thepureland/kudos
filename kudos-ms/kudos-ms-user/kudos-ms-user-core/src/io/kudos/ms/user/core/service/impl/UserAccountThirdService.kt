package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.user.core.dao.UserAccountThirdDao
import io.kudos.ms.user.core.model.po.UserAccountThird
import io.kudos.ms.user.core.service.iservice.IUserAccountThirdService
import org.springframework.stereotype.Service


/**
 * 用户账号第三方绑定业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
open class UserAccountThirdService : BaseCrudService<String, UserAccountThird, UserAccountThirdDao>(), IUserAccountThirdService {


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