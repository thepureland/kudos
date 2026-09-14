package io.kudos.ms.tag.common.assignment.model

import kotlinx.serialization.Serializable

/** Independent sources that may contribute a tag membership. */
@Serializable
enum class TagMembershipSource {
    RULE,
    MANUAL,
    IMPORT,
    DEFAULT,
}
