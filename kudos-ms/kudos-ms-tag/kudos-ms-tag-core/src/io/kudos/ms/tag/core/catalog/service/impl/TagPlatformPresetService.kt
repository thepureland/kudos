package io.kudos.ms.tag.core.catalog.service.impl

import io.kudos.ms.tag.common.catalog.model.TagPresetCopyView
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.catalog.tagset.model.po.TagSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
open class TagPlatformPresetService(
    private val attributeDao: TagAttributeDefinitionDao,
    private val tagSetDao: TagSetDao,
    private val tagDao: TagDefinitionDao,
) {
    open fun copyToTenant(tenantId: String, subjectType: String): TagPresetCopyView {
        require(tenantId != PLATFORM_TENANT_ID) { "Platform presets cannot be copied onto the platform tenant." }
        require(attributeDao.listBySubjectType(tenantId, subjectType).isEmpty()) { "Tenant already has tag attributes for [$subjectType]." }
        require(tagSetDao.listBySubjectType(tenantId, subjectType).isEmpty()) { "Tenant already has tag sets for [$subjectType]." }
        require(tagDao.listBySubjectType(tenantId, subjectType).isEmpty()) { "Tenant already has tags for [$subjectType]." }

        val sourceAttributes = attributeDao.listBySubjectType(PLATFORM_TENANT_ID, subjectType)
        val sourceSets = tagSetDao.listBySubjectType(PLATFORM_TENANT_ID, subjectType)
        val sourceTags = tagDao.listBySubjectType(PLATFORM_TENANT_ID, subjectType)
        require(sourceAttributes.isNotEmpty() || sourceSets.isNotEmpty() || sourceTags.isNotEmpty()) {
            "No platform preset exists for subject type [$subjectType]."
        }

        val attributes = sourceAttributes.map { source ->
            TagAttributeDefinition().apply {
                id = newId()
                this.tenantId = tenantId
                this.subjectType = source.subjectType
                code = source.code
                name = source.name
                description = source.description
                valueType = source.valueType
                cardinality = source.cardinality
                active = source.active
                builtIn = false
                version = 0
            }.also { attributeDao.insert(it) }
        }

        val setIdMap = mutableMapOf<String, String>()
        val sets = sourceSets.map { source ->
            TagSet().apply {
                id = newId().also { setIdMap[source.id] = it }
                this.tenantId = tenantId
                this.subjectType = source.subjectType
                code = source.code
                name = source.name
                cardinality = source.cardinality
                active = source.active
                builtIn = false
                version = 0
            }.also { tagSetDao.insert(it) }
        }

        val tagIdMap = mutableMapOf<String, String>()
        val tags = sourceTags.map { source ->
            TagDefinition().apply {
                id = newId().also { tagIdMap[source.id] = it }
                this.tenantId = tenantId
                this.subjectType = source.subjectType
                code = source.code
                name = source.name
                description = source.description
                tagSetId = source.tagSetId?.let(setIdMap::getValue)
                setPriority = source.setPriority
                manualAssignable = source.manualAssignable
                active = source.active
                builtIn = false
                version = 0
            }.also { tagDao.insert(it) }
        }

        sourceSets.forEach { source ->
            val defaultTagId = source.defaultTagId ?: return@forEach
            val target = sets.first { it.id == setIdMap.getValue(source.id) }
            target.defaultTagId = tagIdMap.getValue(defaultTagId)
            target.version = target.version + 1
            check(tagSetDao.update(target)) { "Failed to copy default tag for set [${source.code}]." }
        }

        return TagPresetCopyView(
            attributes = attributes.map { it.toView() },
            tagSets = sets.map { requireNotNull(tagSetDao.get(it.id)).toView() },
            tags = tags.map { it.toView() },
        )
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        const val PLATFORM_TENANT_ID = "__platform__"
    }
}
