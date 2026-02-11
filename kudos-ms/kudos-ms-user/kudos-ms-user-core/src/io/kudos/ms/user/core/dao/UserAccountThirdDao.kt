package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNull
import io.kudos.ms.user.core.model.po.UserAccountThird
import io.kudos.ms.user.core.model.table.UserAccountThirds
import org.springframework.stereotype.Repository


/**
 * 用户账号第三方绑定数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserAccountThirdDao : BaseCrudDao<String, UserAccountThird, UserAccountThirds>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 按用户ID查询第三方绑定记录
     *
     * @param userId 用户ID
     * @return 第三方绑定记录列表
     */
    fun searchByUserId(userId: String): List<UserAccountThird> {
        val criteria = Criteria(UserAccountThird::userId eq userId)
        return search(criteria)
    }

    /**
     * 按租户+平台+发行方+主体标识查询绑定记录
     *
     * @param tenantId 租户ID
     * @param accountProviderDictCode 账号提供方字典码
     * @param accountProviderIssuer 发行方，可为null
     * @param subject 第三方主体标识
     * @return 绑定记录，不存在返回null
     */
    fun fetchByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird? {
        val criteria = Criteria(UserAccountThird::tenantId eq tenantId)
            .addAnd(UserAccountThird::accountProviderDictCode eq accountProviderDictCode)
            .addAnd(UserAccountThird::subject eq subject)
        if (accountProviderIssuer == null) {
            criteria.addAnd(UserAccountThird::accountProviderIssuer.isNull())
        } else {
            criteria.addAnd(UserAccountThird::accountProviderIssuer eq accountProviderIssuer)
        }
        return search(criteria).firstOrNull()
    }

    //endregion your codes 2

}
