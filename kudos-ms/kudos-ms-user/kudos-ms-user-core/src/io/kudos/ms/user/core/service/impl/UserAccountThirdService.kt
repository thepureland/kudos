package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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
//region your codes 1
open class UserAccountThirdService : BaseCrudService<String, UserAccountThird, UserAccountThirdDao>(), IUserAccountThirdService {
//endregion your codes 1

    //region your codes 2

    override fun getByUserAccountId(userId: String): List<UserAccountThird> {
        val criteria = Criteria(UserAccountThird::userId.name, OperatorEnum.EQ, userId)
        return dao.search(criteria)
    }

    override fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird? {
        val criteria = Criteria(UserAccountThird::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(UserAccountThird::accountProviderDictCode.name, OperatorEnum.EQ, accountProviderDictCode)
            .addAnd(UserAccountThird::subject.name, OperatorEnum.EQ, subject)

        if (accountProviderIssuer == null) {
            criteria.addAnd(UserAccountThird::accountProviderIssuer.name, OperatorEnum.IS_NULL, null)
        } else {
            criteria.addAnd(UserAccountThird::accountProviderIssuer.name, OperatorEnum.EQ, accountProviderIssuer)
        }

        return dao.search(criteria).firstOrNull()
    }

    //endregion your codes 2

}