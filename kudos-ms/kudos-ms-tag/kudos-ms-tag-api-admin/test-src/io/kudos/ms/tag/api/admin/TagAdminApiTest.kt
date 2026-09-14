package io.kudos.ms.tag.api.admin

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.tag.api.admin.controller.*
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagAdminApiTest {
    private val controllers = listOf(
        TagSubjectTypeAdminController::class.java,
        TagAttributeAdminController::class.java,
        TagSetAdminController::class.java,
        TagDefinitionAdminController::class.java,
        TagRuleAdminController::class.java,
        TagAssignmentAdminController::class.java,
        TagRecalculationJobAdminController::class.java,
    )

    @Test
    fun `admin controllers expose the approved roots`() {
        assertEquals(
            setOf(
                "/api/admin/tag/subject-type",
                "/api/admin/tag/attribute",
                "/api/admin/tag/tag-set",
                "/api/admin/tag/tag",
                "/api/admin/tag/rule",
                "/api/admin/tag/assignment",
                "/api/admin/tag/recalculation-job",
            ),
            controllers.map { it.getAnnotation(RequestMapping::class.java).value.single() }.toSet(),
        )
    }

    @Test
    fun `admin operations declare the complete permission matrix`() {
        val permissions = controllers.flatMap { type ->
            type.declaredMethods.mapNotNull { it.getAnnotation(RequiresPermission::class.java)?.value }
        }.toSet()

        assertEquals(
            setOf(
                "tag:subject-type:view",
                "tag:attribute:view", "tag:attribute:manage",
                "tag:definition:view", "tag:definition:manage",
                "tag:rule:view", "tag:rule:manage", "tag:rule:publish",
                "tag:assignment:view", "tag:assignment:manual",
                "tag:job:view", "tag:job:retry", "tag:job:cancel",
            ),
            permissions,
        )
        assertTrue(controllers.all { type -> type.declaredMethods.any { it.isAnnotationPresent(RequiresPermission::class.java) } })
    }

    @Test
    fun `admin boundary does not accept runtime tag-query expressions`() {
        val bodyTypes = controllers.flatMap { it.declaredMethods.toList() }
            .flatMap { method -> method.parameters.toList() }
            .filter { parameter -> parameter.isAnnotationPresent(RequestBody::class.java) }
            .map { it.type }

        assertFalse(TagQueryRequest::class.java in bodyTypes)
        assertFalse(TagQueryExpression::class.java in bodyTypes)
    }
}
