package io.kudos.ms.tag.core.catalog

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.subjecttype.model.po.TagSubjectType
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.catalog.tagset.model.po.TagSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class TagCatalogDaoTest : TagDaoTestSupport() {

    @Test
    fun reusableCodesRemainTenantScoped() {
        val subjectTypeDao = TagSubjectTypeDao()
        val attributeDao = TagAttributeDefinitionDao()
        val tagSetDao = TagSetDao()
        val tagDao = TagDefinitionDao()

        subjectTypeDao.insert(subjectType())
        attributeDao.insert(attribute(ATTRIBUTE_A_ID, "tenant-a"))
        attributeDao.insert(attribute(ATTRIBUTE_B_ID, "tenant-b"))
        tagSetDao.insert(tagSet(SET_A_ID, "tenant-a"))
        tagSetDao.insert(tagSet(SET_B_ID, "tenant-b"))
        tagDao.insert(tag(TAG_A_ID, "tenant-a", SET_A_ID))
        tagDao.insert(tag(TAG_B_ID, "tenant-b", SET_B_ID))

        assertEquals(ATTRIBUTE_A_ID, attributeDao.findByCode("tenant-a", SUBJECT_TYPE, "area")?.id)
        assertEquals(ATTRIBUTE_B_ID, attributeDao.findByCode("tenant-b", SUBJECT_TYPE, "area")?.id)
        assertEquals(listOf(TAG_A_ID), tagDao.listBySubjectType("tenant-a", SUBJECT_TYPE).map { it.id })
        assertEquals(listOf(TAG_B_ID), tagDao.listBySubjectType("tenant-b", SUBJECT_TYPE).map { it.id })
        assertNull(tagDao.findByCode("tenant-c", SUBJECT_TYPE, "large"))
    }

    @Test
    fun serviceFacingUpdateRejectsTagCodeChanges() {
        TagSubjectTypeDao().insert(subjectType())
        TagSetDao().insert(tagSet(SET_A_ID, "tenant-a"))
        val dao = TagDefinitionDao()
        dao.insert(tag(TAG_A_ID, "tenant-a", SET_A_ID))

        val persisted = requireNotNull(dao.findByCode("tenant-a", SUBJECT_TYPE, "large"))
        persisted.code = "renamed"

        assertFailsWith<IllegalArgumentException> { dao.updateCatalog(persisted) }
    }

    private fun subjectType() = TagSubjectType().apply {
        id = SUBJECT_TYPE_ID
        code = SUBJECT_TYPE
        name = "House"
        ownerServiceCode = "estate"
        active = true
        builtIn = false
        version = 0
    }

    private fun attribute(idValue: String, tenant: String) = TagAttributeDefinition().apply {
        id = idValue
        tenantId = tenant
        subjectType = SUBJECT_TYPE
        code = "area"
        name = "Area"
        valueType = TagAttributeType.DECIMAL
        cardinality = TagAttributeCardinality.SINGLE
        active = true
        builtIn = false
        version = 0
    }

    private fun tagSet(idValue: String, tenant: String) = TagSet().apply {
        id = idValue
        tenantId = tenant
        subjectType = SUBJECT_TYPE
        code = "estate.kind"
        name = "Kind"
        cardinality = TagAttributeCardinality.MULTIPLE
        active = true
        builtIn = false
        version = 0
    }

    private fun tag(idValue: String, tenant: String, setId: String) = TagDefinition().apply {
        id = idValue
        tenantId = tenant
        subjectType = SUBJECT_TYPE
        code = "large"
        name = "Large"
        tagSetId = setId
        setPriority = 10
        manualAssignable = true
        active = true
        builtIn = false
        version = 0
    }

    private companion object {
        const val SUBJECT_TYPE = "estate.house"
        const val SUBJECT_TYPE_ID = "00000000-0000-0000-0000-000000000001"
        const val ATTRIBUTE_A_ID = "00000000-0000-0000-0000-000000000101"
        const val ATTRIBUTE_B_ID = "00000000-0000-0000-0000-000000000102"
        const val SET_A_ID = "00000000-0000-0000-0000-000000000201"
        const val SET_B_ID = "00000000-0000-0000-0000-000000000202"
        const val TAG_A_ID = "00000000-0000-0000-0000-000000000301"
        const val TAG_B_ID = "00000000-0000-0000-0000-000000000302"
    }
}
