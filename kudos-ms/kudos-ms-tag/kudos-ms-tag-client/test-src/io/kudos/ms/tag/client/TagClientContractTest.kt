package io.kudos.ms.tag.client

import io.kudos.ms.tag.client.proxy.ITagAssignmentProxy
import io.kudos.ms.tag.client.proxy.ITagAttributeFactProxy
import io.kudos.ms.tag.client.proxy.ITagCatalogProxy
import io.kudos.ms.tag.client.proxy.ITagQueryProxy
import io.kudos.ms.tag.client.init.TagClientAutoConfiguration
import io.kudos.ms.tag.common.assignment.api.ITagAssignmentApi
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentStatus
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.api.ITagCatalogApi
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.common.fact.api.ITagAttributeFactApi
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import io.kudos.ms.tag.common.fact.model.TagAttributeFactStatus
import io.kudos.ms.tag.common.query.api.ITagQueryApi
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${TagClientContractTest.PORT}",
        "spring.http.serviceclient.${TagClientAutoConfiguration.GROUP}.base-url=http://localhost:${TagClientContractTest.PORT}",
    ],
)
@Import(
    TagClientContractTest.MockFactController::class,
    TagClientContractTest.MockAssignmentController::class,
    TagClientContractTest.MockQueryController::class,
    TagClientContractTest.MockCatalogController::class,
)
open class TagClientContractTest {

    @Autowired private lateinit var factProxy: ITagAttributeFactProxy
    @Autowired private lateinit var assignmentProxy: ITagAssignmentProxy
    @Autowired private lateinit var queryProxy: ITagQueryProxy
    @Autowired private lateinit var catalogProxy: ITagCatalogProxy

    @Test
    fun `facts preserve typed values over the shared HTTP contract`() {
        val fact = TagAttributeFact(
            eventId = "event-1",
            subjectKey = key("house-1"),
            attributeCode = "area",
            operation = TagAttributeOperation.SET,
            value = TagAttributeValue.DecimalValue("101.50"),
            occurredAt = Instant.parse("2026-09-14T00:00:00Z"),
            sourceCode = "estate",
        )

        val result = factProxy.submitFacts(listOf(fact))

        assertEquals("decimal:101.50", result.single().errorMessage)
    }

    @Test
    fun `manual assignment and removal round-trip request bodies`() {
        val assigned = assignmentProxy.assignManual(
            AssignManualTagRequest(
                eventId = "assign-1",
                requestId = "request-1",
                subjectKey = key("game-1"),
                tagId = "tag-online",
                operatorId = "operator-1",
                operatorName = "K",
                reason = "verified",
                effectiveUntil = Instant.parse("2026-12-31T00:00:00Z"),
                occurredAt = Instant.parse("2026-09-14T00:00:00Z"),
            )
        )
        val removed = assignmentProxy.removeManual(
            RemoveManualTagRequest(
                eventId = "remove-1",
                requestId = null,
                subjectKey = key("game-1"),
                tagId = "tag-online",
                manualSourceRef = "assign-1",
                operatorId = "operator-1",
                operatorName = null,
                reason = null,
                occurredAt = Instant.parse("2026-09-14T01:00:00Z"),
            )
        )

        assertEquals(setOf("tag-online"), assigned.assignedTagIds)
        assertEquals(setOf("tag-online"), removed.removedTagIds)
    }

    @Test
    fun `query preserves nested sealed expressions`() {
        val page = queryProxy.findSubjects(
            TagQueryRequest(
                tenantId = "tenant-1",
                subjectType = "estate.house",
                expression = TagQueryExpression.AllTags(
                    tagCodes = setOf("room_three", "area_100_plus"),
                    nested = listOf(
                        TagQueryExpression.AnyTags(setOf("feature_terrace", "feature_rooftop"))
                    ),
                ),
                afterSubjectId = "house-0",
                pageSize = 20,
            )
        )

        assertEquals(listOf("house-0:20:3"), page.subjectIds)
    }

    @Test
    fun `recalculation and catalog request params round-trip`() {
        val result = assignmentProxy.recalculateSubjects(
            TagRecalculateSubjectsRequest(listOf(key("person-1")), directRuleLimit = 33)
        )
        val tags = catalogProxy.listTags("tenant-1", "hr.person")

        assertEquals(33, result.directRulesEvaluated)
        assertEquals("tenant-1:hr.person", tags.single().name)
    }

    @RestController
    open class MockFactController : ITagAttributeFactApi {
        override fun submitFacts(facts: List<TagAttributeFact>): List<TagAttributeFactResult> = facts.map {
            val decimal = it.value as TagAttributeValue.DecimalValue
            TagAttributeFactResult(it.eventId, TagAttributeFactStatus.APPLIED, 1, errorMessage = "decimal:${decimal.value}")
        }
    }

    @RestController
    open class MockAssignmentController : ITagAssignmentApi {
        override fun assignManual(request: AssignManualTagRequest) = ManualTagAssignmentResponse(
            request.eventId, ManualTagAssignmentStatus.APPLIED, setOf(request.tagId), emptySet(), 1,
        )

        override fun removeManual(request: RemoveManualTagRequest) = ManualTagAssignmentResponse(
            request.eventId, ManualTagAssignmentStatus.APPLIED, emptySet(), setOf(request.tagId), 2,
        )

        override fun recalculateSubjects(request: TagRecalculateSubjectsRequest) = TagRecalculationResult(
            subjectsProcessed = request.subjectKeys.size,
            directRulesEvaluated = request.directRuleLimit,
            cascadedRulesEvaluated = 0,
            assignmentChanges = 0,
        )
    }

    @RestController
    open class MockQueryController : ITagQueryApi {
        override fun findSubjects(request: TagQueryRequest): TagSubjectPage {
            val expression = request.expression as TagQueryExpression.AllTags
            val count = expression.tagCodes.size +
                (expression.nested.single() as TagQueryExpression.AnyTags).tagCodes.size - 1
            return TagSubjectPage(listOf("${request.afterSubjectId}:${request.pageSize}:$count"))
        }
    }

    @RestController
    open class MockCatalogController : ITagCatalogApi {
        override fun listTags(tenantId: String, subjectType: String) = listOf(
            TagDefinitionView("tag-1", tenantId, subjectType, "online", "$tenantId:$subjectType", null, 0, true)
        )
    }

    companion object {
        const val PORT = "18109"

        private fun key(subjectId: String) = TagSubjectKey("tenant-1", "estate.house", subjectId)
    }
}
