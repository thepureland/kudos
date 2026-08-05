package io.kudos.ms.auth.core.instance.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.instance.model.po.AuthInstanceGrant
import io.kudos.ms.auth.core.instance.model.table.AuthInstanceGrants
import org.springframework.stereotype.Repository
import java.time.LocalDateTime


/**
 * Instance-grant DAO.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthInstanceGrantDao : BaseCrudDao<String, AuthInstanceGrant, AuthInstanceGrants>() {

    /**
     * The live grants a principal holds on one instance — what the decision point consults when a
     * request names one.
     *
     * @param principalId the subject
     * @param resourceType the kind of thing
     * @param instanceId the particular one
     * @param now the instant to evaluate validity windows against
     * @return the applicable grants
     */
    open fun searchLiveGrants(
        principalId: String,
        resourceType: String,
        instanceId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<AuthInstanceGrant> {
        val criteria = Criteria.and(
            AuthInstanceGrant::principalId eq principalId,
            AuthInstanceGrant::resourceType eq resourceType,
            AuthInstanceGrant::instanceId eq instanceId,
        )
        return search(criteria).filter { it.isLiveAt(now) }
    }

    /**
     * Everyone a particular instance is shared with — what every sharing UI needs to render.
     *
     * @param resourceType the kind of thing
     * @param instanceId the particular one
     * @return the live grants on it
     */
    open fun searchLiveGrantsOnInstance(
        resourceType: String,
        instanceId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<AuthInstanceGrant> {
        val criteria = Criteria.and(
            AuthInstanceGrant::resourceType eq resourceType,
            AuthInstanceGrant::instanceId eq instanceId,
        )
        return search(criteria).filter { it.isLiveAt(now) }
    }

    /**
     * Everything currently shared *with* one principal.
     *
     * The other direction from [searchLiveGrantsOnInstance], and it answers a different question:
     * not "who can see this document" but "what has this person been given" — which is what an
     * offboarding review or an access report needs, and what nothing else in the module can produce,
     * because instance shares are deliberately outside the role model.
     *
     * @param principalId the subject
     * @param resourceType narrow to one kind of thing, or null for all of them
     * @param now the instant to evaluate validity windows against
     * @return the live grants held by the principal
     */
    open fun searchLiveGrantsOfPrincipal(
        principalId: String,
        resourceType: String? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<AuthInstanceGrant> {
        val criteria = if (resourceType.isNullOrBlank()) {
            Criteria(AuthInstanceGrant::principalId eq principalId)
        } else {
            Criteria.and(
                AuthInstanceGrant::principalId eq principalId,
                AuthInstanceGrant::resourceType eq resourceType,
            )
        }
        return search(criteria).filter { it.isLiveAt(now) }
    }

    /** Finds an existing grant for the same tuple, so sharing twice updates rather than duplicates. */
    open fun findGrant(
        principalId: String,
        resourceType: String,
        instanceId: String,
        action: String,
    ): AuthInstanceGrant? {
        val criteria = Criteria.and(
            AuthInstanceGrant::principalId eq principalId,
            AuthInstanceGrant::resourceType eq resourceType,
            AuthInstanceGrant::instanceId eq instanceId,
            AuthInstanceGrant::action eq action,
        )
        return search(criteria).firstOrNull()
    }

    /** Revoked or out of its window ⇒ not part of any decision. */
    private fun AuthInstanceGrant.isLiveAt(now: LocalDateTime): Boolean {
        if (revoked == true) return false
        val start = startTime
        val end = endTime
        return (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now))
    }
}
