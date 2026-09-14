package io.kudos.ms.tag.common.attribute.model

import kotlinx.serialization.Serializable

/** Mutations accepted by the attribute-fact ingress contract. */
@Serializable
enum class TagAttributeOperation {
    SET,
    ADD,
    APPEND,
    REMOVE,
    CLEAR,
}
