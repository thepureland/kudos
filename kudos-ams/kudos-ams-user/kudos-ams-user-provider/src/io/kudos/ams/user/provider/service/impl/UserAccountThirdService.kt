package io.kudos.ams.user.provider.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.user.provider.dao.UserAccountThirdDao
import io.kudos.ams.user.provider.model.po.UserAccountThird
import io.kudos.ams.user.provider.service.iservice.IUserAccountThirdService
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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

    override fun getByUserAccountId(userAccountId: String): List<UserAccountThird> {
        val criteria = Criteria(UserAccountThird::userAccountId.name, OperatorEnum.EQ, userAccountId)
        return dao.search(criteria)
    }

    override fun getByProviderSubject(
        tenantId: String,
        subSysDictCode: String,
        accountProviderDictCode: String,
        providerIssuer: String?,
        subject: String
    ): UserAccountThird? {
        val criteria = Criteria(UserAccountThird::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(UserAccountThird::subSysDictCode.name, OperatorEnum.EQ, subSysDictCode)
            .addAnd(UserAccountThird::accountProviderDictCode.name, OperatorEnum.EQ, accountProviderDictCode)
            .addAnd(UserAccountThird::subject.name, OperatorEnum.EQ, subject)

        if (providerIssuer == null) {
            criteria.addAnd(UserAccountThird::providerIssuer.name, OperatorEnum.IS_NULL, null)
        } else {
            criteria.addAnd(UserAccountThird::providerIssuer.name, OperatorEnum.EQ, providerIssuer)
        }

        return dao.search(criteria).firstOrNull()
    }

    //endregion your codes 2

}
