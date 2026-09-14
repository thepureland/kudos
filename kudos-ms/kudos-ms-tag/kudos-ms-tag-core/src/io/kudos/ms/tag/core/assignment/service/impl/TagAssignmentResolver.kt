package io.kudos.ms.tag.core.assignment.service.impl

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.core.assignment.model.TagResolutionCandidate
import io.kudos.ms.tag.core.runtime.port.ResolvedAssignment
import org.springframework.stereotype.Component

/** Resolves independent membership evidence into the one materialized view queried by applications. */
@Component
open class TagAssignmentResolver {

    fun resolve(candidates: List<TagResolutionCandidate>): List<ResolvedAssignment> = candidates
        .groupBy { it.tagSetId ?: "standalone:${it.membership.tagId}" }
        .values
        .flatMap(::resolveGroup)
        .sortedBy { it.tagId }

    private fun resolveGroup(group: List<TagResolutionCandidate>): List<ResolvedAssignment> {
        val cardinality = group.first().cardinality
        require(group.all { it.cardinality == cardinality }) { "A tag set cannot mix cardinalities." }
        val eligible = group.filter { it.membership.source != TagMembershipSource.DEFAULT }
            .ifEmpty { group.filter { it.membership.source == TagMembershipSource.DEFAULT } }
        if (eligible.isEmpty()) return emptyList()
        return if (cardinality == TagAttributeCardinality.SINGLE) {
            listOf(eligible.sortedWith(CANDIDATE_ORDER).first().toResolved())
        } else {
            eligible.groupBy { it.membership.tagId }
                .values
                .map { sameTag -> sameTag.sortedWith(CANDIDATE_ORDER).first().toResolved() }
        }
    }

    private fun TagResolutionCandidate.toResolved() = ResolvedAssignment(
        tagId = membership.tagId,
        tagSetId = tagSetId,
        winningSource = membership.source,
        membershipVersion = membership.membershipVersion,
        evaluatedRuleVersion = membership.ruleVersion,
        effectiveFrom = membership.effectiveFrom,
        effectiveUntil = membership.effectiveUntil,
    )

    private companion object {
        val SOURCE_PRIORITY = mapOf(
            TagMembershipSource.MANUAL to 4,
            TagMembershipSource.RULE to 3,
            TagMembershipSource.IMPORT to 2,
            TagMembershipSource.DEFAULT to 1,
        )
        val CANDIDATE_ORDER = compareByDescending<TagResolutionCandidate> { SOURCE_PRIORITY.getValue(it.membership.source) }
            .thenByDescending { it.setPriority }
            .thenByDescending { it.membership.membershipVersion }
            .thenBy { it.membership.tagId }
    }
}
