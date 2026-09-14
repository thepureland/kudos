package io.kudos.ms.tag.core.assignment.model

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.core.runtime.port.TagMembershipView

/** A membership enriched with the catalog fields needed for deterministic set resolution. */
data class TagResolutionCandidate(
    val membership: TagMembershipView,
    val tagSetId: String?,
    val cardinality: TagAttributeCardinality,
    val setPriority: Int,
)
