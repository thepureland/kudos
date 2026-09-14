package io.kudos.ms.tag.core.catalog

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagSetCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.service.impl.TagPlatformPresetService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

internal class TagCatalogServiceTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val tagDao = TagDefinitionDao()
    private val service = TagCatalogService(subjectTypeDao, attributeDao, tagSetDao, tagDao)

    @Test
    fun validatesNamespacedSubjectTypesAndTypedAttributes() {
        assertFailsWith<IllegalArgumentException> {
            service.registerSubjectType(RegisterTagSubjectTypeCommand("Estate.House", "House", "estate"))
        }
        service.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "House", "estate"))
        val attribute = service.createAttribute(
            CreateTagAttributeCommand("tenant-a", SUBJECT_TYPE, "area", "Area", TagAttributeType.DECIMAL, TagAttributeCardinality.SINGLE)
        )
        assertEquals(TagAttributeType.DECIMAL, attribute.valueType)
    }

    @Test
    fun enforcesSetModeAndSubjectTypeConsistency() {
        service.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "House", "estate"))
        service.registerSubjectType(RegisterTagSubjectTypeCommand("game.room", "Game", "game"))
        val multiple = service.createTagSet(
            CreateTagSetCommand("tenant-a", SUBJECT_TYPE, "features", "Features", TagAttributeCardinality.MULTIPLE)
        )
        val single = service.createTagSet(
            CreateTagSetCommand("tenant-a", SUBJECT_TYPE, "size", "Size", TagAttributeCardinality.SINGLE)
        )
        val tag = service.createTag(CreateTagCommand("tenant-a", SUBJECT_TYPE, "large", "Large", single.id, 10, true))

        assertFailsWith<IllegalArgumentException> { service.setDefaultTag("tenant-a", multiple.id, tag.id) }
        assertFailsWith<IllegalArgumentException> {
            service.createTag(CreateTagCommand("tenant-a", "game.room", "bad", "Bad", single.id, 0, true))
        }
        service.setDefaultTag("tenant-a", single.id, tag.id)
        assertEquals(tag.id, requireNotNull(tagSetDao.get(single.id)).defaultTagId)
    }

    @Test
    fun platformPresetIsCopiedIntoTenantOwnedRows() {
        service.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "House", "estate"))
        val presetSet = service.createTagSet(
            CreateTagSetCommand(TagPlatformPresetService.PLATFORM_TENANT_ID, SUBJECT_TYPE, "size", "Size", TagAttributeCardinality.SINGLE, builtIn = true)
        )
        val presetTag = service.createTag(
            CreateTagCommand(TagPlatformPresetService.PLATFORM_TENANT_ID, SUBJECT_TYPE, "large", "Large", presetSet.id, 10, true, builtIn = true)
        )
        service.setDefaultTag(TagPlatformPresetService.PLATFORM_TENANT_ID, presetSet.id, presetTag.id)

        val copied = TagPlatformPresetService(attributeDao, tagSetDao, tagDao).copyToTenant("tenant-a", SUBJECT_TYPE)

        assertEquals(1, copied.tagSets.size)
        assertEquals(1, copied.tags.size)
        assertNotEquals(presetSet.id, copied.tagSets.single().id)
        assertNotEquals(presetTag.id, copied.tags.single().id)
        assertEquals("tenant-a", copied.tags.single().tenantId)
        assertEquals(copied.tags.single().id, copied.tagSets.single().defaultTagId)
    }

    private companion object {
        const val SUBJECT_TYPE = "estate.house"
    }
}
