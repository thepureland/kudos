package io.kudos.ms.tag.core.catalog.service.iservice

import io.kudos.ms.tag.common.catalog.model.*

interface ITagCatalogService {
    fun registerSubjectType(command: RegisterTagSubjectTypeCommand): TagSubjectTypeView
    fun createAttribute(command: CreateTagAttributeCommand): TagAttributeView
    fun createTagSet(command: CreateTagSetCommand): TagSetView
    fun createTag(command: CreateTagCommand): TagDefinitionView
    fun updateTag(command: UpdateTagCommand): TagDefinitionView
    fun setDefaultTag(tenantId: String, tagSetId: String, tagId: String): TagSetView
}
