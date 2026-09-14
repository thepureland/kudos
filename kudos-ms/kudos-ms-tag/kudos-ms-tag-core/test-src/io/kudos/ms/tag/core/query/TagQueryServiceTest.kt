package io.kudos.ms.tag.core.query

import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AllTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AnyTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.NotTags
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.query.service.impl.TagQueryService
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignment
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class TagQueryServiceTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val tagDao = TagDefinitionDao()
    private val subjectDao = TagSubjectDao()
    private val assignmentDao = TagAssignmentDao()
    private val index = RdbTagAssignmentIndex(assignmentDao, TagAssignmentEventDao(), tagDao, tagSetDao)
    private val service = TagQueryService(tagDao, index)
    private val catalog = TagCatalogService(subjectTypeDao, attributeDao, tagSetDao, tagDao)

    @Test
    fun queriesMaterializedTagsWithBooleanExpressionsAndExpiry() {
        registerTypes()
        val tags = createTags(
            SUBJECT_TYPE,
            "three-bedroom",
            "area-100-plus",
            "terrace",
            "rooftop",
            "suspended",
        )
        subject("house-1", tags, "three-bedroom", "area-100-plus", "terrace")
        subject("house-2", tags, "three-bedroom", "area-100-plus", "rooftop")
        subject("house-3", tags, "three-bedroom", "terrace")
        subject("house-4", tags, "suspended")
        subject("house-5", tags)
        subject("house-6", tags, "suspended", expired = true)
        subject("house-1", tags, "three-bedroom", tenantId = OTHER_TENANT)

        val matchingHomes = service.findSubjects(
            TagQueryRequest(
                TENANT,
                SUBJECT_TYPE,
                AllTags(
                    tagCodes = setOf("three-bedroom", "area-100-plus"),
                    nested = listOf(AnyTags(setOf("terrace", "rooftop"))),
                ),
            )
        )
        val notSuspended = service.findSubjects(
            TagQueryRequest(TENANT, SUBJECT_TYPE, NotTags(AnyTags(setOf("suspended"))))
        )

        assertEquals(listOf("house-1", "house-2"), matchingHomes.subjectIds)
        assertNull(matchingHomes.nextSubjectId)
        assertEquals(listOf("house-1", "house-2", "house-3", "house-5", "house-6"), notSuspended.subjectIds)
    }

    @Test
    fun supportsStableSubjectIdKeysetPagination() {
        registerTypes()
        val tags = createTags(SUBJECT_TYPE, "eligible")
        listOf("house-b", "house-c", "house-d").forEach { subject(it, tags, "eligible") }

        val first = service.findSubjects(TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(setOf("eligible")), pageSize = 2))
        subject("house-bb", tags)
        val second = service.findSubjects(
            TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(setOf("eligible")), first.nextSubjectId, pageSize = 2)
        )

        assertEquals(listOf("house-b", "house-c"), first.subjectIds)
        assertEquals("house-c", first.nextSubjectId)
        assertEquals(listOf("house-d"), second.subjectIds)
        assertNull(second.nextSubjectId)
        assertEquals(listOf("house-b", "house-c", "house-d"), first.subjectIds + second.subjectIds)
    }

    @Test
    fun rejectsUnknownCrossTypeAndOversizedQueries() {
        registerTypes()
        createTags(SUBJECT_TYPE, "known")
        createTags(OTHER_SUBJECT_TYPE, "other-type")

        assertError(TagErrorCode.QUERY_TAG_NOT_FOUND) {
            service.findSubjects(TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(setOf("missing"))))
        }
        assertError(TagErrorCode.SUBJECT_TYPE_MISMATCH) {
            service.findSubjects(TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(setOf("other-type"))))
        }
        assertError(TagErrorCode.QUERY_LIMIT_EXCEEDED) {
            service.findSubjects(TagQueryRequest(TENANT, SUBJECT_TYPE, nestedAll(11)))
        }
        assertError(TagErrorCode.QUERY_LIMIT_EXCEEDED) {
            service.findSubjects(
                TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags((1..101).map { "tag-$it" }.toSet()))
            )
        }
        assertError(TagErrorCode.QUERY_LIMIT_EXCEEDED) {
            TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(setOf("known")), pageSize = 501)
        }
    }

    private fun registerTypes() {
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "House", "estate"))
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(OTHER_SUBJECT_TYPE, "Game", "game"))
    }

    private fun createTags(subjectType: String, vararg codes: String): Map<String, String> = codes.associateWith { code ->
        catalog.createTag(CreateTagCommand(TENANT, subjectType, code, code)).id
    }

    private fun subject(
        subjectId: String,
        tags: Map<String, String>,
        vararg codes: String,
        tenantId: String = TENANT,
        expired: Boolean = false,
    ) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        check(subjectDao.insertSubject(TagSubject().apply {
            this.subjectId = subjectId
            this.tenantId = tenantId
            subjectType = SUBJECT_TYPE
            displayName = null
            profileJson = null
            stateVersion = 0
            firstSeenTime = now
            updateTime = now
        }))
        codes.forEach { code ->
            check(assignmentDao.insertAssignment(TagAssignment().apply {
                tagId = tags.getValue(code)
                this.tenantId = tenantId
                subjectType = SUBJECT_TYPE
                this.subjectId = subjectId
                exclusiveSetId = null
                assignmentVersion = 1
                evaluatedRuleVersion = null
                materializedTime = now
                effectiveFrom = null
                effectiveUntil = if (expired) now.minusMinutes(1) else null
                updateTime = now
            }))
        }
    }

    private fun nestedAll(depth: Int): AllTags {
        var expression = AllTags(setOf("known"))
        repeat(depth - 1) { expression = AllTags(nested = listOf(expression)) }
        return expression
    }

    private fun assertError(expected: TagErrorCode, block: () -> Unit) {
        assertEquals(expected, assertFailsWith<TagValidationException> { block() }.errorCode)
    }

    private companion object {
        const val TENANT = "tenant-a"
        const val OTHER_TENANT = "tenant-b"
        const val SUBJECT_TYPE = "estate.house"
        const val OTHER_SUBJECT_TYPE = "game.game"
    }
}
