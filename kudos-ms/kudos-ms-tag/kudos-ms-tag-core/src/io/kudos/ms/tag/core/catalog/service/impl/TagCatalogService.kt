package io.kudos.ms.tag.core.catalog.service.impl

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.catalog.model.*
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.subjecttype.model.po.TagSubjectType
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.catalog.tagset.model.po.TagSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
open class TagCatalogService(
    private val subjectTypeDao: TagSubjectTypeDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val tagSetDao: TagSetDao,
    private val tagDao: TagDefinitionDao,
) : ITagCatalogService {

    override fun registerSubjectType(command: RegisterTagSubjectTypeCommand): TagSubjectTypeView {
        require(SUBJECT_TYPE_CODE.matches(command.code)) { "Subject type code must be a lowercase dotted namespace." }
        require(subjectTypeDao.findByCode(command.code) == null) { "Subject type [${command.code}] already exists." }
        val entity = TagSubjectType().apply {
            id = newId()
            code = command.code
            name = command.name
            ownerServiceCode = command.ownerServiceCode
            description = command.description
            active = true
            builtIn = command.builtIn
            version = 0
        }
        subjectTypeDao.insert(entity)
        return entity.toView()
    }

    override fun createAttribute(command: CreateTagAttributeCommand): TagAttributeView {
        requireSubjectType(command.subjectType)
        require(CATALOG_CODE.matches(command.code)) { "Invalid attribute code [${command.code}]." }
        require(attributeDao.findByCode(command.tenantId, command.subjectType, command.code) == null) {
            "Attribute [${command.code}] already exists for this tenant and subject type."
        }
        val entity = TagAttributeDefinition().apply {
            id = newId()
            tenantId = command.tenantId
            subjectType = command.subjectType
            code = command.code
            name = command.name
            description = command.description
            valueType = command.valueType
            cardinality = command.cardinality
            active = true
            builtIn = command.builtIn
            version = 0
        }
        attributeDao.insert(entity)
        return entity.toView()
    }

    override fun createTagSet(command: CreateTagSetCommand): TagSetView {
        requireSubjectType(command.subjectType)
        require(CATALOG_CODE.matches(command.code)) { "Invalid tag-set code [${command.code}]." }
        require(tagSetDao.findByCode(command.tenantId, command.subjectType, command.code) == null) {
            "Tag set [${command.code}] already exists for this tenant and subject type."
        }
        val entity = TagSet().apply {
            id = newId()
            tenantId = command.tenantId
            subjectType = command.subjectType
            code = command.code
            name = command.name
            cardinality = command.cardinality
            active = true
            builtIn = command.builtIn
            version = 0
        }
        tagSetDao.insert(entity)
        return entity.toView()
    }

    override fun createTag(command: CreateTagCommand): TagDefinitionView {
        requireSubjectType(command.subjectType)
        require(CATALOG_CODE.matches(command.code)) { "Invalid tag code [${command.code}]." }
        require(command.setPriority >= 0) { "Tag-set priority must be non-negative." }
        require(tagDao.findByCode(command.tenantId, command.subjectType, command.code) == null) {
            "Tag [${command.code}] already exists for this tenant and subject type."
        }
        command.tagSetId?.let { setId ->
            val set = requireNotNull(tagSetDao.get(setId)) { "Tag set [$setId] does not exist." }
            require(set.tenantId == command.tenantId && set.subjectType == command.subjectType) {
                "Tag and tag set must belong to the same tenant and subject type."
            }
        }
        val entity = TagDefinition().apply {
            id = newId()
            tenantId = command.tenantId
            subjectType = command.subjectType
            code = command.code
            name = command.name
            description = command.description
            tagSetId = command.tagSetId
            setPriority = command.setPriority
            manualAssignable = command.manualAssignable
            active = true
            builtIn = command.builtIn
            version = 0
        }
        tagDao.insert(entity)
        return entity.toView()
    }

    override fun updateTag(command: UpdateTagCommand): TagDefinitionView {
        val entity = requireNotNull(tagDao.get(command.id)) { "Tag [${command.id}] does not exist." }
        require(entity.tenantId == command.tenantId) { "Tag does not belong to tenant [${command.tenantId}]." }
        require(entity.version == command.expectedVersion) { "Tag was concurrently modified." }
        require(command.setPriority >= 0) { "Tag-set priority must be non-negative." }
        entity.name = command.name
        entity.description = command.description
        entity.setPriority = command.setPriority
        entity.manualAssignable = command.manualAssignable
        entity.active = command.active
        entity.version = entity.version + 1
        check(tagDao.updateCatalog(entity)) { "Tag [${command.id}] was not updated." }
        return entity.toView()
    }

    override fun setDefaultTag(tenantId: String, tagSetId: String, tagId: String): TagSetView {
        val set = requireNotNull(tagSetDao.get(tagSetId)) { "Tag set [$tagSetId] does not exist." }
        val tag = requireNotNull(tagDao.get(tagId)) { "Tag [$tagId] does not exist." }
        require(set.tenantId == tenantId && tag.tenantId == tenantId) { "Tag set and tag must belong to tenant [$tenantId]." }
        require(set.cardinality == TagAttributeCardinality.SINGLE) { "Only SINGLE tag sets can define a default tag." }
        require(tag.tagSetId == set.id && tag.subjectType == set.subjectType) { "Default tag must belong to the tag set." }
        set.defaultTagId = tag.id
        set.version = set.version + 1
        check(tagSetDao.update(set)) { "Tag set [$tagSetId] was not updated." }
        return set.toView()
    }

    private fun requireSubjectType(code: String) {
        require(subjectTypeDao.findByCode(code) != null) { "Subject type [$code] is not registered." }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        val SUBJECT_TYPE_CODE = Regex("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+$")
        val CATALOG_CODE = Regex("^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$")
    }
}

internal fun TagSubjectType.toView() = TagSubjectTypeView(id, code, name, ownerServiceCode)
internal fun TagAttributeDefinition.toView() = TagAttributeView(id, tenantId, subjectType, code, name, valueType, cardinality)
internal fun TagSet.toView() = TagSetView(id, tenantId, subjectType, code, name, cardinality, defaultTagId)
internal fun TagDefinition.toView() = TagDefinitionView(id, tenantId, subjectType, code, name, tagSetId, setPriority, manualAssignable)
