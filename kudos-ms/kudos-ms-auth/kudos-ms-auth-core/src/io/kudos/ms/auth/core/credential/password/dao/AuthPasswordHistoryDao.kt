package io.kudos.ms.auth.core.credential.password.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.credential.password.model.po.AuthPasswordHistory
import io.kudos.ms.auth.core.credential.password.model.table.AuthPasswordHistories
import org.springframework.stereotype.Repository

@Repository
open class AuthPasswordHistoryDao :
    BaseCrudDao<String, AuthPasswordHistory, AuthPasswordHistories>() {

    open fun findRecent(
        tenantId: String,
        userId: String,
        purpose: String,
        limit: Int,
    ): List<AuthPasswordHistory> {
        if (limit <= 0) return emptyList()
        return findAll(tenantId, userId, purpose)
            .sortedWith(compareByDescending<AuthPasswordHistory> { it.recordedAt }.thenByDescending { it.id })
            .take(limit)
    }

    open fun trimToSize(
        tenantId: String,
        userId: String,
        purpose: String,
        retained: Int,
    ): Int {
        val obsoleteIds = findAll(tenantId, userId, purpose)
            .sortedWith(compareByDescending<AuthPasswordHistory> { it.recordedAt }.thenByDescending { it.id })
            .drop(retained.coerceAtLeast(0))
            .map(AuthPasswordHistory::id)
        return if (obsoleteIds.isEmpty()) 0 else batchDelete(obsoleteIds)
    }

    open fun deleteByUserId(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthPasswordHistory::userId eq userId))

    private fun findAll(tenantId: String, userId: String, purpose: String): List<AuthPasswordHistory> =
        search(
            Criteria(AuthPasswordHistory::tenantId eq tenantId)
                .addAnd(AuthPasswordHistory::userId eq userId)
                .addAnd(AuthPasswordHistory::purpose eq purpose)
        )
}
