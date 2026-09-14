package io.kudos.ms.tag.api.internal

import io.kudos.ms.tag.api.internal.controller.TagAssignmentInternalController
import io.kudos.ms.tag.api.internal.controller.TagAttributeFactInternalController
import io.kudos.ms.tag.api.internal.controller.TagCatalogInternalController
import io.kudos.ms.tag.api.internal.controller.TagQueryInternalController
import io.kudos.ms.tag.common.assignment.api.ITagAssignmentApi
import io.kudos.ms.tag.common.assignment.model.*
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.catalog.api.ITagCatalogApi
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.common.fact.api.ITagAttributeFactApi
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import io.kudos.ms.tag.common.fact.model.TagAttributeFactStatus
import io.kudos.ms.tag.common.query.api.ITagQueryApi
import io.kudos.ms.tag.common.query.model.*
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.security.TagSubjectWriteGuard
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class TagInternalApiTest {
    private val key = TagSubjectKey("tenant", "estate.house", "house-1")

    @Test
    fun `controllers delegate shared contracts without altering results`() {
        val tenantGuard = RecordingTenantGuard()
        val writeGuard = RecordingSubjectWriteGuard()
        val factResult = listOf(TagAttributeFactResult("event", TagAttributeFactStatus.APPLIED, 4))
        val facts = listOf(TagAttributeFact("event", key, "area", TagAttributeOperation.CLEAR, null, Instant.EPOCH, "estate"))
        val factController = TagAttributeFactInternalController(
            object : ITagAttributeFactApi { override fun submitFacts(facts: List<TagAttributeFact>) = factResult },
            writeGuard,
        )
        val assignmentResult = ManualTagAssignmentResponse(
            "event", ManualTagAssignmentStatus.APPLIED, setOf("tag-1"), emptySet(), 1,
        )
        val assignmentController = TagAssignmentInternalController(
            object : ITagAssignmentApi {
                override fun assignManual(request: AssignManualTagRequest) = assignmentResult
                override fun removeManual(request: RemoveManualTagRequest) = assignmentResult
                override fun recalculateSubjects(request: TagRecalculateSubjectsRequest) =
                    TagRecalculationResult(1, 2, 3, 4)
            },
            tenantGuard,
        )
        val page = TagSubjectPage(listOf("house-1"))
        val queryController = TagQueryInternalController(
            object : ITagQueryApi { override fun findSubjects(request: TagQueryRequest) = page },
            tenantGuard,
        )
        val tags = listOf(TagDefinitionView("id", "tenant", "estate.house", "large", "Large", null, 0, true))
        val catalogController = TagCatalogInternalController(
            object : ITagCatalogApi { override fun listTags(tenantId: String, subjectType: String) = tags },
            tenantGuard,
        )

        assertSame(factResult, factController.submitFacts(facts))
        assertSame(
            assignmentResult,
            assignmentController.assignManual(
                AssignManualTagRequest("event", null, key, "tag-1", "operator", null, null, null, Instant.EPOCH)
            ),
        )
        assertSame(
            page,
            queryController.findSubjects(TagQueryRequest("tenant", "estate.house", TagQueryExpression.AllTags(setOf("large")))),
        )
        assertSame(tags, catalogController.listTags("tenant", "estate.house"))
        assertEquals(listOf(key), writeGuard.keys)
        assertEquals(listOf("tenant", "tenant", "tenant"), tenantGuard.tenants)
    }

    @Test
    fun `internal controllers declare no mapping annotations of their own`() {
        val types = listOf(
            TagAttributeFactInternalController::class.java,
            TagAssignmentInternalController::class.java,
            TagQueryInternalController::class.java,
            TagCatalogInternalController::class.java,
        )

        types.forEach { type ->
            assertFalse(type.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping::class.java))
            type.declaredMethods.forEach { method ->
                assertFalse(
                    method.annotations.any { it.annotationClass.qualifiedName?.startsWith("org.springframework.web.bind.annotation") == true },
                    "${type.simpleName}.${method.name} must inherit its HTTP mapping from the shared interface",
                )
            }
        }
        assertEquals(4, types.size)
    }

    private class RecordingTenantGuard : TagTenantAccessGuard() {
        val tenants = mutableListOf<String>()
        override fun requireTenant(tenantId: String) {
            tenants += tenantId
        }
    }

    private class RecordingSubjectWriteGuard : TagSubjectWriteGuard() {
        val keys = mutableListOf<TagSubjectKey>()
        override fun requireWrite(key: TagSubjectKey) {
            keys += key
        }
    }
}
