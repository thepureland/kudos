package io.kudos.ms.tag.core.scenario

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.core.TagDaoTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EstateTagScenarioTest : TagDaoTestSupport() {
    @Test
    fun `finds three bedroom large houses with a terrace or rooftop using tags only`() {
        val fixture = TagScenarioFixture()
        fixture.configureEstate("tenant-a")
        fixture.submit("tenant-a", "estate.house", "matching", "bedrooms" to int(3), "area" to decimal("120"), "terrace" to bool(true), "rooftop" to bool(false))
        fixture.submit("tenant-a", "estate.house", "no-outdoor", "bedrooms" to int(3), "area" to decimal("130"), "terrace" to bool(false), "rooftop" to bool(false))
        fixture.submit("tenant-a", "estate.house", "too-small", "bedrooms" to int(3), "area" to decimal("80"), "terrace" to bool(true), "rooftop" to bool(false))
        fixture.processPending()

        val result = fixture.query(
            "tenant-a", "estate.house",
            TagQueryExpression.AllTags(
                setOf("three_bedrooms", "area_100_plus"),
                listOf(TagQueryExpression.AnyTags(setOf("has_terrace", "has_rooftop"))),
            ),
        )

        assertEquals(listOf("matching"), result)
    }

    private fun int(value: Long) = TagAttributeValue.IntegerValue(value)
    private fun decimal(value: String) = TagAttributeValue.DecimalValue(value)
    private fun bool(value: Boolean) = TagAttributeValue.BooleanValue(value)
}
