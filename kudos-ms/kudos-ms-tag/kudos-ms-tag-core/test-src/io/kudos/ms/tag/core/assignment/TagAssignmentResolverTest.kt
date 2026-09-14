package io.kudos.ms.tag.core.assignment

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.core.assignment.model.TagResolutionCandidate
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentResolver
import io.kudos.ms.tag.core.runtime.port.TagMembershipView
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TagAssignmentResolverTest {

    private val resolver = TagAssignmentResolver()

    @Test
    fun singleSetUsesSourcePriorityThenTagPriorityVersionAndStableId() {
        val candidates = listOf(
            candidate("default", TagMembershipSource.DEFAULT, priority = 999, version = 99),
            candidate("import", TagMembershipSource.IMPORT, priority = 100, version = 20),
            candidate("rule", TagMembershipSource.RULE, priority = 10, version = 30),
            candidate("manual-low", TagMembershipSource.MANUAL, priority = 1, version = 1),
            candidate("manual-high-old", TagMembershipSource.MANUAL, priority = 5, version = 2),
            candidate("manual-high-new-b", TagMembershipSource.MANUAL, priority = 5, version = 3),
            candidate("manual-high-new-a", TagMembershipSource.MANUAL, priority = 5, version = 3),
        )

        val winners = resolver.resolve(candidates)

        assertEquals(listOf("manual-high-new-a"), winners.map { it.tagId })
        assertEquals(TagMembershipSource.MANUAL, winners.single().winningSource)
        assertEquals(3, winners.single().membershipVersion)
    }

    @Test
    fun multipleSetKeepsEveryNonDefaultTagAndConsolidatesDuplicateSources() {
        val candidates = listOf(
            candidate("default", TagMembershipSource.DEFAULT, cardinality = TagAttributeCardinality.MULTIPLE),
            candidate("alpha", TagMembershipSource.RULE, cardinality = TagAttributeCardinality.MULTIPLE, version = 1),
            candidate("alpha", TagMembershipSource.MANUAL, cardinality = TagAttributeCardinality.MULTIPLE, version = 2),
            candidate("beta", TagMembershipSource.IMPORT, cardinality = TagAttributeCardinality.MULTIPLE, version = 3),
        )

        val winners = resolver.resolve(candidates)

        assertEquals(listOf("alpha", "beta"), winners.map { it.tagId })
        assertEquals(TagMembershipSource.MANUAL, winners.first().winningSource)
    }

    @Test
    fun defaultWinsOnlyWhenASetHasNoNonDefaultCandidate() {
        val winners = resolver.resolve(
            listOf(candidate("fallback", TagMembershipSource.DEFAULT))
        )

        assertEquals(listOf("fallback"), winners.map { it.tagId })
        assertEquals(TagMembershipSource.DEFAULT, winners.single().winningSource)
    }

    private fun candidate(
        tagId: String,
        source: TagMembershipSource,
        priority: Int = 0,
        version: Long = 1,
        cardinality: TagAttributeCardinality = TagAttributeCardinality.SINGLE,
    ) = TagResolutionCandidate(
        membership = TagMembershipView(
            id = "$tagId-${source.name}",
            tagId = tagId,
            source = source,
            sourceRef = "$tagId-${source.name}",
            ruleVersion = if (source == TagMembershipSource.RULE) 1 else null,
            membershipVersion = version,
            effectiveFrom = null,
            effectiveUntil = null,
        ),
        tagSetId = "set-1",
        cardinality = cardinality,
        setPriority = priority,
    )
}
