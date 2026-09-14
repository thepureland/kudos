package io.kudos.ms.tag.client

import io.kudos.ms.tag.client.fallback.TagAssignmentFallback
import io.kudos.ms.tag.client.fallback.TagAttributeFactFallback
import io.kudos.ms.tag.client.fallback.TagCatalogFallback
import io.kudos.ms.tag.client.fallback.TagQueryFallback
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.error.TagServiceUnavailableException
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TagFallbackTest {
    private val cause = IllegalStateException("remote unavailable")

    @Test
    fun `every fallback fails closed with operation and cause`() {
        assertUnavailable("submitFacts") {
            TagAttributeFactFallback().submitFacts(cause, emptyList<TagAttributeFact>())
        }
        assertUnavailable("assignManual") {
            TagAssignmentFallback().assignManual(
                cause,
                AssignManualTagRequest(
                    "event", null, key(), "tag-1", "operator", null, null, null, Instant.EPOCH,
                ),
            )
        }
        assertUnavailable("removeManual") {
            TagAssignmentFallback().removeManual(
                cause,
                RemoveManualTagRequest(
                    "event", null, key(), "tag-1", "assignment-1", "operator", null, null, Instant.EPOCH,
                ),
            )
        }
        assertUnavailable("recalculateSubjects") {
            TagAssignmentFallback().recalculateSubjects(
                cause,
                TagRecalculateSubjectsRequest(listOf(key())),
            )
        }
        assertUnavailable("findSubjects") {
            TagQueryFallback().findSubjects(
                cause,
                TagQueryRequest("tenant", "estate.house", TagQueryExpression.AllTags(setOf("online"))),
            )
        }
        assertUnavailable("listTags") {
            TagCatalogFallback().listTags(cause, "tenant", "estate.house")
        }

        val withoutCause = assertFailsWith<TagServiceUnavailableException> {
            TagCatalogFallback().listTags("tenant", "estate.house")
        }
        assertEquals("listTags", withoutCause.operation)
        assertEquals(null, withoutCause.cause)
    }

    private fun assertUnavailable(operation: String, block: () -> Unit) {
        val error = assertFailsWith<TagServiceUnavailableException>(block = block)
        assertEquals(operation, error.operation)
        assertSame(cause, error.cause)
    }

    private fun key() = TagSubjectKey("tenant", "estate.house", "subject-1")
}
