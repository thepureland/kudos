package io.kudos.ms.tag.core.scenario

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.core.TagDaoTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GameTagScenarioTest : TagDaoTestSupport() {
    @Test
    fun `finds gomoku one versus one games that can be played online`() {
        val fixture = TagScenarioFixture()
        fixture.configureGame("tenant-a")
        fixture.submit("tenant-a", "game.catalog", "online-gomoku", "gameplay" to text("gomoku"), "mode" to text("1v1"), "online" to bool(true))
        fixture.submit("tenant-a", "game.catalog", "offline-gomoku", "gameplay" to text("gomoku"), "mode" to text("1v1"), "online" to bool(false))
        fixture.submit("tenant-a", "game.catalog", "online-chess", "gameplay" to text("chess"), "mode" to text("1v1"), "online" to bool(true))
        fixture.processPending()

        val result = fixture.query(
            "tenant-a", "game.catalog",
            TagQueryExpression.AllTags(setOf("gomoku", "one_v_one", "online_play")),
        )

        assertEquals(listOf("online-gomoku"), result)
    }

    private fun text(value: String) = TagAttributeValue.StringValue(value)
    private fun bool(value: Boolean) = TagAttributeValue.BooleanValue(value)
}
