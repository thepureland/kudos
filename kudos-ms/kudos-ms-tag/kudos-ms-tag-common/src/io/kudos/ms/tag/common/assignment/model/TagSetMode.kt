package io.kudos.ms.tag.common.assignment.model

import kotlinx.serialization.Serializable

/** Cardinality policy for a set of related tags. */
@Serializable
enum class TagSetMode {
    SINGLE,
    MULTIPLE,
}
