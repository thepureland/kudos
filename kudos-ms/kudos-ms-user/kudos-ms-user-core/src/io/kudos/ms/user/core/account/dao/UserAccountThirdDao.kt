package io.kudos.ms.user.core.account.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNull
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.model.table.UserAccountThirds
import org.springframework.stereotype.Repository


/**
 * User account third-party binding DAO.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class UserAccountThirdDao : BaseCrudDao<String, UserAccountThird, UserAccountThirds>() {


    /**
     * Query third-party binding records by user id.
     *
     * @param userId user id
     * @return list of third-party binding records
     */
    fun searchByUserId(userId: String): List<UserAccountThird> {
        val criteria = Criteria(UserAccountThird::userId eq userId)
        return search(criteria)
    }

    /**
     * Query a binding record by tenant + provider + issuer + subject.
     *
     * @param tenantId tenant id
     * @param accountProviderDictCode account provider dict code
     * @param accountProviderIssuer issuer; may be null
     * @param subject third-party subject identifier
     * @return binding record, or null if not found
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


}
