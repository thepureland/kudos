package io.kudos.tools.codegen.core.merge

import java.io.File
import kotlin.test.*

/**
 * test for PrivateContentEraser
 *
 * @author K
 * @since 1.0.0
 */
internal class PrivateContentEraserTest {

    @Test
    fun erasesMarkersWithLfLineEndings() {
        val file = newTempFile(
            "//region append IMPARTIBLE codes 1\n" +
                "val a = 1\n" +
                "//endregion append IMPARTIBLE codes 1\n"
        )
        PrivateContentEraser.erase(file)

        val content = file.readText()
        assertFalse(content.contains("region append"))
        assertTrue(content.contains("val a = 1"))
    }

    @Test
    fun erasesMarkersWithCrLfLineEndings() {
        val file = newTempFile(
            "//region append PARTIBLE codes 2\r\n" +
                "import x.Y\r\n" +
                "//endregion append PARTIBLE codes 2\r\n"
        )
        PrivateContentEraser.erase(file)

        val content = file.readText()
        assertFalse(content.contains("region append"))
        assertTrue(content.contains("import x.Y"))
    }

    @Test
    fun keepsUserRegionMarkersUntouched() {
        val file = newTempFile(
            "//region your codes 1\n" +
                "val keep = true\n" +
                "//endregion your codes 1\n"
        )
        PrivateContentEraser.erase(file)

        val content = file.readText()
        assertTrue(content.contains("//region your codes 1"))
        assertTrue(content.contains("//endregion your codes 1"))
    }

    private fun newTempFile(content: String): File =
        File.createTempFile("eraser-test", ".kt").apply {
            deleteOnExit()
            writeText(content)
        }
}
