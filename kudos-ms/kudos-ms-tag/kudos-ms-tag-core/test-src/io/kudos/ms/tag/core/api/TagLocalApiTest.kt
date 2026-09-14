package io.kudos.ms.tag.core.api

import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentStatus
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.catalog.model.*
import io.kudos.ms.tag.common.fact.model.TagAttributeFactStatus
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentResult
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentStatus
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand
import io.kudos.ms.tag.core.assignment.service.iservice.ITagAssignmentService
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import io.kudos.ms.tag.core.fact.model.AttributeFactResult
import io.kudos.ms.tag.core.fact.model.AttributeFactStatus
import io.kudos.ms.tag.core.fact.service.iservice.ITagAttributeFactService
import io.kudos.ms.tag.core.query.service.iservice.ITagQueryService
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import io.kudos.ms.tag.core.runtime.port.LeasedRecalculationJob
import io.kudos.ms.tag.core.runtime.service.iservice.ITagRecalculationService
import io.kudos.ms.tag.core.runtime.service.iservice.RecalculationSummary
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TagLocalApiTest {
    private val key = TagSubjectKey("tenant", "estate.house", "house-1")

    @Test
    fun `fact API maps the internal result without losing status`() {
        val api = TagAttributeFactApi(object : ITagAttributeFactService {
            override fun submitFact(fact: TagAttributeFact) = AttributeFactResult(fact.eventId, AttributeFactStatus.APPLIED, 8)
            override fun submitFacts(facts: List<TagAttributeFact>, sliceSize: Int) =
                facts.map { submitFact(it) }
        })

        val fact = TagAttributeFact("fact-1", key, "area", TagAttributeOperation.CLEAR, null, Instant.EPOCH, "test")
        val result = api.submitFacts(listOf(fact))

        assertEquals(TagAttributeFactStatus.APPLIED, result.single().status)
        assertEquals(8, result.single().stateVersion)
    }

    @Test
    fun `assignment API maps wire command and internal delta`() {
        var received: AssignManualTagCommand? = null
        val assignment = object : ITagAssignmentService {
            override fun assignManual(command: AssignManualTagCommand): ManualAssignmentResult {
                received = command
                return ManualAssignmentResult(
                    command.eventId,
                    ManualAssignmentStatus.APPLIED,
                    AssignmentDelta(setOf(command.tagId), emptySet(), 7),
                )
            }

            override fun removeManual(command: RemoveManualTagCommand) = error("unused")
            override fun applyRuleResult(key: TagSubjectKey, tagId: String, ruleId: String, ruleVersion: Long, matched: Boolean, causeRef: String) = error("unused")
        }
        val api = TagAssignmentApi(assignment, recalculationSummary())

        val response = api.assignManual(
            AssignManualTagRequest("event-1", "request-1", key, "tag-1", "operator", "K", "reason", null, Instant.EPOCH)
        )

        assertEquals("request-1", received?.requestId)
        assertEquals(ManualTagAssignmentStatus.APPLIED, response.status)
        assertEquals(setOf("tag-1"), response.assignedTagIds)
        assertEquals(7, response.assignmentVersion)
    }

    @Test
    fun `query and recalculation delegate to the local services`() {
        val queryApi = TagQueryApi(object : ITagQueryService {
            override fun findSubjects(request: TagQueryRequest) = TagSubjectPage(listOf(request.tenantId))
        })
        val request = TagQueryRequest("tenant", "estate.house", TagQueryExpression.AllTags(setOf("online")))
        val assignmentApi = TagAssignmentApi(unusedAssignment(), recalculationSummary())

        assertEquals(listOf("tenant"), queryApi.findSubjects(request).subjectIds)
        assertEquals(
            42,
            assignmentApi.recalculateSubjects(TagRecalculateSubjectsRequest(listOf(key), 12)).assignmentChanges,
        )
    }

    @Test
    fun `catalog API preserves tenant and subject scope`() {
        val service = object : ITagCatalogService {
            override fun listTags(tenantId: String, subjectType: String) = listOf(
                TagDefinitionView("id", tenantId, subjectType, "online", "Online", null, 0, true)
            )

            override fun registerSubjectType(command: RegisterTagSubjectTypeCommand) = error("unused")
            override fun createAttribute(command: CreateTagAttributeCommand) = error("unused")
            override fun createTagSet(command: CreateTagSetCommand) = error("unused")
            override fun createTag(command: CreateTagCommand) = error("unused")
            override fun updateTag(command: UpdateTagCommand) = error("unused")
            override fun setDefaultTag(tenantId: String, tagSetId: String, tagId: String) = error("unused")
        }

        val tag = TagCatalogApi(service).listTags("tenant", "estate.house").single()

        assertEquals("tenant", tag.tenantId)
        assertEquals("estate.house", tag.subjectType)
    }

    private fun recalculationSummary() = object : ITagRecalculationService {
        override fun requestIncremental(keys: Collection<TagSubjectKey>, reason: String) = emptyList<String>()
        override fun recalculateSubjectNow(keys: Collection<TagSubjectKey>, directRuleLimit: Int) =
            RecalculationSummary(keys.size, directRuleLimit, 3, 42)
        override fun process(job: LeasedRecalculationJob) = error("unused")
    }

    private fun unusedAssignment() = object : ITagAssignmentService {
        override fun assignManual(command: AssignManualTagCommand) = error("unused")
        override fun removeManual(command: RemoveManualTagCommand) = error("unused")
        override fun applyRuleResult(key: TagSubjectKey, tagId: String, ruleId: String, ruleVersion: Long, matched: Boolean, causeRef: String) = error("unused")
    }
}
