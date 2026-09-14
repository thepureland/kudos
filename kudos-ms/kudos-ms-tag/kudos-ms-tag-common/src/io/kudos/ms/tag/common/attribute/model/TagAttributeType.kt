package io.kudos.ms.tag.common.attribute.model

import kotlinx.serialization.Serializable

/** Value types supported by tag attributes in the first RDB implementation. */
@Serializable
enum class TagAttributeType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    DATETIME,
}
