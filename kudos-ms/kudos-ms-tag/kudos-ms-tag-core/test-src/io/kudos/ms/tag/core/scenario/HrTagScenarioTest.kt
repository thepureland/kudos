package io.kudos.ms.tag.core.scenario

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.core.TagDaoTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HrTagScenarioTest : TagDaoTestSupport() {
    @Test
    fun `finds people aged from thirty through forty`() {
        val fixture = TagScenarioFixture()
        fixture.configureHr("tenant-a")
        fixture.submit("tenant-a", "hr.person", "age-30", "age" to int(30))
        fixture.submit("tenant-a", "hr.person", "age-40", "age" to int(40))
        fixture.submit("tenant-a", "hr.person", "age-41", "age" to int(41))
        fixture.processPending()

        assertEquals(
            listOf("age-30", "age-40"),
            fixture.query("tenant-a", "hr.person", TagQueryExpression.AllTags(setOf("age_30_40"))),
        )
    }

    private fun int(value: Long) = TagAttributeValue.IntegerValue(value)
}
