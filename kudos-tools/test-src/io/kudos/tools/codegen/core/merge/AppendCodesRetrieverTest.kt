package io.kudos.tools.codegen.core.merge

import kotlin.test.*

/**
 * test for AppendCodesRetriever
 *
 * @author K
 * @since 1.0.0
 */
internal class AppendCodesRetrieverTest {

    @Test
    fun returnsEmptyWhenNoAppendRegion() {
        assertTrue(AppendCodesRetriever("class Demo").retrieve().isEmpty())
    }

    @Test
    fun capturesPartibleType() {
        val content = buildString {
            appendLine("//region append PARTIBLE codes 1")
            appendLine("import x.Y")
            appendLine("//endregion append PARTIBLE codes 1")
        }
        val result = AppendCodesRetriever(content).retrieve()

        assertEquals(setOf(1), result.keys)
        assertEquals(AppendCodeType.PARTIBLE, result.getValue(1).first)
        assertTrue(result.getValue(1).second.contains("import x.Y"))
    }

    @Test
    fun capturesImpartibleType() {
        val content = buildString {
            appendLine("//region append IMPARTIBLE codes 2")
            appendLine("fun block() {}")
            appendLine("//endregion append IMPARTIBLE codes 2")
        }
        val result = AppendCodesRetriever(content).retrieve()

        assertEquals(AppendCodeType.IMPARTIBLE, result.getValue(2).first)
        assertTrue(result.getValue(2).second.contains("fun block() {}"))
    }

    @Test
    fun capturesMixedTypesInOneFile() {
        val content = buildString {
            appendLine("//region append PARTIBLE codes 1")
            appendLine("a")
            appendLine("//endregion append PARTIBLE codes 1")
            appendLine("//region append IMPARTIBLE codes 2")
            appendLine("b")
            appendLine("//endregion append IMPARTIBLE codes 2")
        }
        val result = AppendCodesRetriever(content).retrieve()

        assertEquals(setOf(1, 2), result.keys)
        assertEquals(AppendCodeType.PARTIBLE, result.getValue(1).first)
        assertEquals(AppendCodeType.IMPARTIBLE, result.getValue(2).first)
    }
}
