package io.kudos.ms.user.core.account.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.core.account.model.po.UserAccountThirdAudit
import io.kudos.ms.user.core.account.model.table.UserAccountThirdAudits
import org.springframework.stereotype.Repository

/** Append-only binding lifecycle audit DAO. */
@Repository
open class UserAccountThirdAuditDao :
    BaseCrudDao<String, UserAccountThirdAudit, UserAccountThirdAudits>() {

    fun searchByBindingId(bindingId: String): List<UserAccountThirdAudit> =
        search(Criteria(UserAccountThirdAudit::bindingId eq bindingId))
}
