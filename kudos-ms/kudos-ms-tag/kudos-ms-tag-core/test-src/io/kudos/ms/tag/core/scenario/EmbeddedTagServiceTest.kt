package io.kudos.ms.tag.core.scenario

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.core.TagDaoTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class EmbeddedTagServiceTest : TagDaoTestSupport() {
    @Test
    fun `embedded core local APIs work without client or discovery and preserve tenant isolation`() {
        val fixture = TagScenarioFixture()
        fixture.configureHr("tenant-a")
        fixture.configureHr("tenant-b")
        fixture.submitThroughLocalApi("tenant-a", "hr.person", "same-id", "age" to TagAttributeValue.IntegerValue(35))
        fixture.submitThroughLocalApi("tenant-b", "hr.person", "same-id", "age" to TagAttributeValue.IntegerValue(50))
        fixture.processPending()

        val expression = TagQueryExpression.AllTags(setOf("age_30_40"))
        assertEquals(listOf("same-id"), fixture.queryThroughLocalApi("tenant-a", "hr.person", expression))
        assertEquals(emptyList(), fixture.queryThroughLocalApi("tenant-b", "hr.person", expression))
        assertFalse(runCatching { Class.forName("io.kudos.ms.tag.client.init.TagClientAutoConfiguration") }.isSuccess)
    }
}
