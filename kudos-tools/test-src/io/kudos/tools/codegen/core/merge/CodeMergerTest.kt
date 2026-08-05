package io.kudos.tools.codegen.core.merge

import java.io.File
import kotlin.test.*

/**
 * test for CodeMerger
 *
 * Covers: user-region restoration, import merging (user-added imports kept, template imports not
 * duplicated, early return when the user added none), PARTIBLE append with per-line deduplication,
 * IMPARTIBLE append with whole-block deduplication, non-.kt files skipping import merging,
 * CRLF region markers, and the import-insertion fallbacks (after package / at file start).
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

    @Test
    fun mergeAppendsPartibleCodesLineByLineWithDeduplication() {
        val file = newTempFile(
            """
            //region your codes 1
            import a.A
            //endregion your codes 1
            """.trimIndent()
        )
        val merger = CodeMerger(file)
        file.writeText(
            """
            //region your codes 1
            //endregion your codes 1
            //region append PARTIBLE codes 1
            import a.A
            import b.B
            //endregion append PARTIBLE codes 1
            """.trimIndent()
        )

        merger.merge()

        val content = file.readText()
        assertTrue(content.contains("import b.B"), "new partible line must be appended")
        // the user already had "import a.A" in the region; PARTIBLE dedup must not add it again there
        val regionBody = content.substringAfter("//region your codes 1")
            .substringBefore("//endregion your codes 1")
        assertEquals(1, Regex("import a\\.A").findAll(regionBody).count(), "duplicate line must be skipped")
    }

    @Test
    fun mergeAppendsImpartibleBlockOnlyWhenAbsent() {
        val file = newTempFile(
            """
            //region your codes 2
            existing block
            //endregion your codes 2
            """.trimIndent()
        )
        val merger = CodeMerger(file)
        file.writeText(
            """
            //region your codes 2
            //endregion your codes 2
            //region append IMPARTIBLE codes 2
            existing block
            //endregion append IMPARTIBLE codes 2
            """.trimIndent()
        )

        merger.merge()

        val regionBody = file.readText().substringAfter("//region your codes 2")
            .substringBefore("//endregion your codes 2")
        assertEquals(
            1, Regex("existing block").findAll(regionBody).count(),
            "an impartible block already present in the user code must not be appended again"
        )
    }

    @Test
    fun mergeAppendsImpartibleBlockWhenMissing() {
        val file = newTempFile(
            """
            //region your codes 2
            user only
            //endregion your codes 2
            """.trimIndent()
        )
        val merger = CodeMerger(file)
        file.writeText(
            """
            //region your codes 2
            //endregion your codes 2
            //region append IMPARTIBLE codes 2
            fresh block
            //endregion append IMPARTIBLE codes 2
            """.trimIndent()
        )

        merger.merge()

        val regionBody = file.readText().substringAfter("//region your codes 2")
            .substringBefore("//endregion your codes 2")
        assertTrue(regionBody.contains("user only"))
        assertTrue(regionBody.contains("fresh block"))
    }

    @Test
    fun mergeSkipsImportHandlingForNonKotlinFiles() {
        val file = File.createTempFile("merger-test", ".html").apply {
            deleteOnExit()
            writeText(
                "import old.Style\n" +
                    "<!--//region your codes 1-->\n" +
                    "<div>user</div>\n" +
                    "<!--//endregion your codes 1-->\n"
            )
        }
        val merger = CodeMerger(file)
        file.writeText(
            "<!--//region your codes 1-->\n" +
                "<!--//endregion your codes 1-->\n"
        )

        merger.merge()

        val content = file.readText()
        assertTrue(content.contains("<div>user</div>"), "html user region must be restored")
        assertFalse(content.contains("import old.Style"), "import merging must be skipped for non-.kt files")
    }

    @Test
    fun mergeReturnsEarlyWhenUserAddedNoImports() {
        val file = newTempFile(
            """
            import a.A

            //region your codes 1
            val custom = 1
            //endregion your codes 1
            """.trimIndent()
        )
        val merger = CodeMerger(file)
        val regenerated =
            """
            import a.A
            import c.C

            //region your codes 1
            //endregion your codes 1
            """.trimIndent()
        file.writeText(regenerated)

        merger.merge()

        val content = file.readText()
        assertEquals(1, Regex("import a\\.A").findAll(content).count())
        assertEquals(1, Regex("import c\\.C").findAll(content).count())
    }

    @Test
    fun mergeInsertsImportAtFileStartWhenNoPackageNorImports() {
        val file = newTempFile(
            "import b.B\n" +
                "//region your codes 1\n" +
                "val custom = 1\n" +
                "//endregion your codes 1\n"
        )
        val merger = CodeMerger(file)
        file.writeText(
            "//region your codes 1\n" +
                "//endregion your codes 1\n"
        )

        merger.merge()

        assertTrue(file.readText().startsWith("import b.B"), "import must be inserted at the file start")
    }

    @Test
    fun mergeInsertsImportAtFileStartWhenPackageHasNoLineBreak() {
        val file = newTempFile(
            "import b.B\n" +
                "//region your codes 1\n" +
                "//endregion your codes 1\n"
        )
        val merger = CodeMerger(file)
        file.writeText(
            "//region your codes 1\n" +
                "//endregion your codes 1\n" +
                "package demo"
        )

        merger.merge()

        assertTrue(file.readText().startsWith("import b.B"))
    }

    @Test
    fun mergeHandlesCrLfRegionMarkers() {
        val file = newTempFile(
            "//region your codes 1\r\n" +
                "val custom = 7\r\n" +
                "//endregion your codes 1\r\n"
        )
        val merger = CodeMerger(file)
        file.writeText(
            "//region your codes 1\r\n" +
                "//endregion your codes 1\r\n"
        )

        merger.merge()

        assertTrue(file.readText().contains("val custom = 7"), "CRLF user region must be restored")
    }

    private fun newTempFile(content: String): File =
        File.createTempFile("merger-test", ".kt").apply {
            deleteOnExit()
            writeText(content)
        }
}
