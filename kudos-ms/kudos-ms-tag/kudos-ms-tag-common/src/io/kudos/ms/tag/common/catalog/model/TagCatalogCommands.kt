package io.kudos.ms.tag.common.catalog.model

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import kotlinx.serialization.Serializable

@Serializable
data class RegisterTagSubjectTypeCommand(
    val code: String,
    val name: String,
    val ownerServiceCode: String,
    val description: String? = null,
    val builtIn: Boolean = false,
)

@Serializable
data class CreateTagAttributeCommand(
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val valueType: TagAttributeType,
    val cardinality: TagAttributeCardinality,
    val description: String? = null,
    val builtIn: Boolean = false,
)

@Serializable
data class CreateTagSetCommand(
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val cardinality: TagAttributeCardinality,
    val builtIn: Boolean = false,
)

@Serializable
data class CreateTagCommand(
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val tagSetId: String? = null,
    val setPriority: Int = 0,
    val manualAssignable: Boolean = true,
    val description: String? = null,
    val builtIn: Boolean = false,
)

@Serializable
data class UpdateTagCommand(
    val id: String,
    val tenantId: String,
    val name: String,
    val description: String? = null,
    val setPriority: Int,
    val manualAssignable: Boolean,
    val active: Boolean,
    val expectedVersion: Long,
)
