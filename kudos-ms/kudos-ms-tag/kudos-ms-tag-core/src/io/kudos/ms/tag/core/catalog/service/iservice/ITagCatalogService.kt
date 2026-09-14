package io.kudos.ms.tag.core.catalog.service.iservice

import io.kudos.ms.tag.common.catalog.model.*

interface ITagCatalogService {
    fun listTags(tenantId: String, subjectType: String): List<TagDefinitionView>
    fun registerSubjectType(command: RegisterTagSubjectTypeCommand): TagSubjectTypeView
    fun createAttribute(command: CreateTagAttributeCommand): TagAttributeView
    fun createTagSet(command: CreateTagSetCommand): TagSetView
    fun createTag(command: CreateTagCommand): TagDefinitionView
    fun updateTag(command: UpdateTagCommand): TagDefinitionView
    fun setDefaultTag(tenantId: String, tagSetId: String, tagId: String): TagSetView
}
