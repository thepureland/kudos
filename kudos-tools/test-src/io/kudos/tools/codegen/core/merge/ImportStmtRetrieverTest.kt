package io.kudos.tools.codegen.core.merge

import kotlin.test.*

/**
 * test for ImportStmtRetriever
 *
 * @author K
 * @since 1.0.0
 */
internal class ImportStmtRetrieverTest {

    @Test
    fun retrievesAllImportLines() {
        val content = """
            package io.kudos.demo

            import io.kudos.base.lang.string.StringKit
            import java.time.LocalDate
            import kotlin.test.assertEquals

            class Demo
        """.trimIndent()

        val imports = ImportStmtRetriever(content).retrieveImports()

        assertEquals(
            listOf(
                "import io.kudos.base.lang.string.StringKit",
                "import java.time.LocalDate",
                "import kotlin.test.assertEquals",
            ),
            imports,
        )
    }

    @Test
    fun returnsEmptyWhenNoImports() {
        val content = "package io.kudos.demo\n\nclass Demo"
        assertTrue(ImportStmtRetriever(content).retrieveImports().isEmpty())
    }

    @Test
    fun preservesOrderAndDuplicates() {
        val content = "import a.B\nimport a.B\nimport c.D"
        assertEquals(listOf("import a.B", "import a.B", "import c.D"), ImportStmtRetriever(content).retrieveImports())
    }
}
