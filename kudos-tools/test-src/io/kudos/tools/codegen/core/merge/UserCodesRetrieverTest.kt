package io.kudos.tools.codegen.core.merge

import kotlin.test.*

/**
 * test for UserCodesRetriever
 *
 * @author K
 * @since 1.0.0
 */
internal class UserCodesRetrieverTest {

    @Test
    fun returnsEmptyWhenNoRegion() {
        assertTrue(UserCodesRetriever("class Demo").retrieve().isEmpty())
    }

    @Test
    fun capturesSingleRegionKeyedByNumber() {
        val content = buildString {
            appendLine("//region your codes 1")
            appendLine("val custom = 1")
            appendLine("//endregion your codes 1")
        }
        val result = UserCodesRetriever(content).retrieve()

        assertEquals(setOf(1), result.keys)
        assertTrue(result.getValue(1).contains("val custom = 1"))
    }

    @Test
    fun capturesMultipleRegions() {
        val content = buildString {
            appendLine("//region your codes 1")
            appendLine("import a.B")
            appendLine("//endregion your codes 1")
            appendLine("class Demo {")
            appendLine("//region your codes 2")
            appendLine("fun foo() {}")
            appendLine("//endregion your codes 2")
            appendLine("}")
        }
        val result = UserCodesRetriever(content).retrieve()

        assertEquals(setOf(1, 2), result.keys)
        assertTrue(result.getValue(1).contains("import a.B"))
        assertTrue(result.getValue(2).contains("fun foo() {}"))
    }

    @Test
    fun supportsHtmlCommentStyleMarkers() {
        val content = buildString {
            appendLine("<!--//region your codes 1-->")
            appendLine("<div>user</div>")
            appendLine("<!--//endregion your codes 1-->")
        }
        val result = UserCodesRetriever(content).retrieve()

        assertEquals(setOf(1), result.keys)
        assertTrue(result.getValue(1).contains("<div>user</div>"))
    }
}
