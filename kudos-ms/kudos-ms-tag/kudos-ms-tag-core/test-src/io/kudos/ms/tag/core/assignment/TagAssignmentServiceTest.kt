package io.kudos.ms.tag.core.assignment

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagSetCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentStatus
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentValidationException
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentResolver
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentService
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentTransactionExecutor
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagManualAssignmentEventDao
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.rdb.RdbTagMembershipStore
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class TagAssignmentServiceTest : TagDaoTestSupport() {

    private val subjectTypeDao = TagSubjectTypeDao()
    private val attributeDao = TagAttributeDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val tagDao = TagDefinitionDao()
    private val membershipDao = TagMembershipDao()
    private val assignmentDao = TagAssignmentDao()
    private val assignmentEventDao = TagAssignmentEventDao()
    private val manualEventDao = TagManualAssignmentEventDao()
    private val membershipStore = RdbTagMembershipStore(membershipDao)
    private val assignmentIndex = RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao)
    private val catalog = TagCatalogService(subjectTypeDao, attributeDao, tagSetDao, tagDao)
    private val service = TagAssignmentService(
        tagDao,
        tagSetDao,
        TagSubjectDao(),
        membershipStore,
        assignmentIndex,
        TagAssignmentResolver(),
        assignmentEventDao,
        manualEventDao,
        TagAssignmentTransactionExecutor(),
    )

    @Test
    fun membershipSourcesRemainIndependentBySourceReference() {
        val (_, _, assignable) = prepareCatalog()
        val key = key("source-isolation")
        insertSubject(key)
        val sources = listOf(
            TagMembershipSource.RULE to "rule-v1",
            TagMembershipSource.MANUAL to "manual-event",
            TagMembershipSource.IMPORT to "import-batch",
            TagMembershipSource.DEFAULT to "default-set",
        )
        sources.forEachIndexed { index, (source, sourceRef) ->
            membershipStore.replace(key, assignable, source, sourceRef, true, (index + 1).toLong())
        }

        membershipStore.replace(key, assignable, TagMembershipSource.RULE, "rule-v1", false, 5)

        assertEquals(
            setOf(TagMembershipSource.MANUAL, TagMembershipSource.IMPORT, TagMembershipSource.DEFAULT),
            membershipStore.listActive(key).map { it.source }.toSet(),
        )
        assertEquals(4, membershipDao.list(key).size)
    }

    @Test
    fun manualCommandsAreIdempotentAuditedAndRemovalFallsBackToDefault() {
        val (setId, defaultTagId, assignableTagId) = prepareCatalog()
        val key = key("manual")
        val assignEventId = UUID.randomUUID().toString()
        val expiry = Instant.ofEpochSecond(Instant.now().epochSecond + 3600)
        val assign = AssignManualTagCommand(
            eventId = assignEventId,
            requestId = "request-17",
            subjectKey = key,
            tagId = assignableTagId,
            operatorId = "operator-7",
            operatorName = "Ada",
            reason = "verified by support",
            effectiveUntil = expiry,
            occurredAt = expiry.minusSeconds(7200),
        )

        assertEquals(ManualAssignmentStatus.APPLIED, service.assignManual(assign).status)
        assertEquals(ManualAssignmentStatus.DUPLICATE, service.assignManual(assign).status)
        assertFailsWith<ManualAssignmentValidationException> {
            service.assignManual(assign.copy(reason = "different payload"))
        }.also { assertEquals(TagErrorCode.IDEMPOTENCY_CONFLICT, it.errorCode) }
        assertEquals(listOf(assignableTagId), assignmentDao.list(key).map { it.tagId })
        assertEquals(expiry, membershipStore.listActive(key).single { it.source == TagMembershipSource.MANUAL }.effectiveUntil)

        val audit = requireNotNull(manualEventDao.get(assignEventId))
        assertEquals("request-17", audit.requestId)
        assertEquals("operator-7", audit.operatorId)
        assertEquals("Ada", audit.operatorName)
        assertEquals("verified by support", audit.reason)
        assertEquals(expiry, requireNotNull(audit.effectiveUntil).toInstant(ZoneOffset.UTC))
        assertEquals("APPLIED", audit.processStatus)

        val removeEventId = UUID.randomUUID().toString()
        val removed = service.removeManual(
            RemoveManualTagCommand(
                eventId = removeEventId,
                requestId = "request-18",
                subjectKey = key,
                tagId = assignableTagId,
                manualSourceRef = assignEventId,
                operatorId = "operator-8",
                operatorName = "Grace",
                reason = "request withdrawn",
                occurredAt = Instant.parse("2026-09-14T02:00:00Z"),
            )
        )

        assertEquals(ManualAssignmentStatus.APPLIED, removed.status)
        assertEquals(listOf(defaultTagId), assignmentDao.list(key).map { it.tagId })
        assertEquals(setOf(assignableTagId), removed.delta.removedTagIds)
        assertEquals(setOf(defaultTagId), removed.delta.assignedTagIds)
        val deltaEvents = assignmentEventDao.list(key).filter { it.causeRef == removeEventId }
        assertEquals(setOf("ASSIGNED", "REMOVED"), deltaEvents.map { it.operation }.toSet())
        assertTrue(deltaEvents.all { it.assignmentVersion == removed.delta.assignmentVersion })
        assertEquals(setId, assignmentDao.list(key).single().exclusiveSetId)
    }

    @Test
    fun manualAssignmentRejectsTagsThatDisallowItWithoutWritingAudit() {
        val (_, _, _) = prepareCatalog()
        val locked = requireNotNull(tagDao.findByCode(TENANT, SUBJECT_TYPE, "locked"))
        val eventId = UUID.randomUUID().toString()

        val error = assertFailsWith<ManualAssignmentValidationException> {
            service.assignManual(
                AssignManualTagCommand(
                    eventId,
                    "request-locked",
                    key("locked"),
                    locked.id,
                    "operator-9",
                    "Lin",
                    "not permitted",
                    null,
                    Instant.parse("2026-09-14T03:00:00Z"),
                )
            )
        }

        assertEquals(TagErrorCode.MANUAL_ASSIGNMENT_NOT_ALLOWED, error.errorCode)
        assertEquals(null, manualEventDao.get(eventId))
    }

    @Test
    fun multipleSetMaterializesAllTagsWithoutUsingTheExclusiveSetColumn() {
        prepareCatalog()
        val set = catalog.createTagSet(
            CreateTagSetCommand(TENANT, SUBJECT_TYPE, "interests", "Interests", TagAttributeCardinality.MULTIPLE)
        )
        val first = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "reading", "Reading", set.id))
        val second = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "swimming", "Swimming", set.id))
        val key = key("multiple")

        listOf(first.id, second.id).forEachIndexed { index, tagId ->
            service.assignManual(
                AssignManualTagCommand(
                    UUID.randomUUID().toString(),
                    "multiple-$index",
                    key,
                    tagId,
                    "operator",
                    null,
                    null,
                    null,
                    Instant.parse("2026-09-14T0${index + 1}:00:00Z"),
                )
            )
        }

        val assignments = assignmentDao.list(key)
        assertEquals(setOf(first.id, second.id), assignments.map { it.tagId }.toSet())
        assertTrue(assignments.all { it.exclusiveSetId == null })
    }

    private fun prepareCatalog(): Triple<String, String, String> {
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "Person", "hr"))
        val set = catalog.createTagSet(CreateTagSetCommand(TENANT, SUBJECT_TYPE, "status", "Status", TagAttributeCardinality.SINGLE))
        val fallback = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "fallback", "Fallback", set.id))
        val assignable = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "active", "Active", set.id, setPriority = 10))
        catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "locked", "Locked", set.id, manualAssignable = false))
        catalog.setDefaultTag(TENANT, set.id, fallback.id)
        return Triple(set.id, fallback.id, assignable.id)
    }

    private fun insertSubject(key: TagSubjectKey) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        check(TagSubjectDao().insertSubject(TagSubject().apply {
            subjectId = key.subjectId
            tenantId = key.tenantId
            subjectType = key.subjectType
            displayName = null
            profileJson = null
            stateVersion = 0
            firstSeenTime = now
            updateTime = now
        }))
    }

    private fun key(subjectId: String) = TagSubjectKey(TENANT, SUBJECT_TYPE, subjectId)

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
    }
}
