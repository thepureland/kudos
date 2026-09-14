package io.kudos.ms.tag.common.catalog.model

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import kotlinx.serialization.Serializable

@Serializable
data class TagSubjectTypeView(val id: String, val code: String, val name: String, val ownerServiceCode: String)

@Serializable
data class TagAttributeView(
    val id: String,
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val valueType: TagAttributeType,
    val cardinality: TagAttributeCardinality,
)

@Serializable
data class TagSetView(
    val id: String,
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val cardinality: TagAttributeCardinality,
    val defaultTagId: String? = null,
)

@Serializable
data class TagDefinitionView(
    val id: String,
    val tenantId: String,
    val subjectType: String,
    val code: String,
    val name: String,
    val tagSetId: String?,
    val setPriority: Int,
    val manualAssignable: Boolean,
)

@Serializable
data class TagPresetCopyView(
    val attributes: List<TagAttributeView>,
    val tagSets: List<TagSetView>,
    val tags: List<TagDefinitionView>,
)
