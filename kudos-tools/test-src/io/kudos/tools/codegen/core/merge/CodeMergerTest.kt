package io.kudos.tools.codegen.core.merge

import java.io.File
import kotlin.test.*

/**
 * test for CodeMerger
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeMergerTest {

    @Test
    fun mergeRestoresUserCodeAndCustomImports() {
        val file = newTempFile(
            """
            package demo

            import a.A
            import b.B

            //region your codes 1
            val custom = 1
            //endregion your codes 1
            """.trimIndent()
        )
        // CodeMerger must be constructed BEFORE the file is overwritten by the new generation
        val merger = CodeMerger(file)
        file.writeText(
            """
            package demo

            import a.A

            //region your codes 1
            //endregion your codes 1
            """.trimIndent()
        )

        merger.merge()

        val content = file.readText()
        assertTrue(content.contains("val custom = 1"), "user code inside the region must be restored")
        assertTrue(content.contains("import b.B"), "user-added import must be merged back")
        assertEquals(1, Regex("import a\\.A").findAll(content).count(), "template import must not be duplicated")
    }

    @Test
    fun mergeDoesNotCrashWhenNewFileHasNoImports() {
        val file = newTempFile(
            """
            package demo

            import b.B

            //region your codes 1
            val custom = 1
            //endregion your codes 1
            """.trimIndent()
        )
        val merger = CodeMerger(file)
        file.writeText(
            """
            package demo

            //region your codes 1
            //endregion your codes 1
            """.trimIndent()
        )

        merger.merge()

        val content = file.readText()
        assertTrue(content.contains("import b.B"), "user import must be inserted even without template imports")
        assertTrue(content.indexOf("package demo") < content.indexOf("import b.B"), "import must come after package")
    }

    private fun newTempFile(content: String): File =
        File.createTempFile("merger-test", ".kt").apply {
            deleteOnExit()
            writeText(content)
        }
}
