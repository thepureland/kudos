package io.kudos.ms.tag.common.attribute.model

import kotlinx.serialization.Serializable

/** Whether an attribute holds one value or a set of values. */
@Serializable
enum class TagAttributeCardinality {
    SINGLE,
    MULTIPLE,
}
