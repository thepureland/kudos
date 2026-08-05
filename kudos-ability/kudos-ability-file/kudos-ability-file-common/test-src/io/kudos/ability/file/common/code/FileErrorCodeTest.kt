package io.kudos.ability.file.common.code

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [FileErrorCode].
 *
 * Covers: code/defaultDisplayText of every enum entry, code uniqueness,
 * blank i18nKeyPrefix (so displayText falls back to defaultDisplayText),
 * and getMessage formatting with and without parameters.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class FileErrorCodeTest {

    @Test
    fun codesAndTextsAreAsDefined() {
        assertEquals("FS00000000", FileErrorCode.FILE_INVALID_ACCESS_KEY.code)
        assertEquals("FS00000001", FileErrorCode.FILE_ACCESS_ERROR.code)
        assertEquals("FS00000002", FileErrorCode.FILE_ACCESS_DENY.code)
        assertEquals("FS00000003", FileErrorCode.FILE_NO_EXISTS.code)
        assertEquals("FS00000004", FileErrorCode.FILE_UPLOAD_FAIL.code)
        assertEquals("FS00000005", FileErrorCode.FILE_DELETE_FAIL.code)
        assertEquals("File does not exist", FileErrorCode.FILE_NO_EXISTS.defaultDisplayText)
    }

    @Test
    fun codesAreUnique() {
        val codes = FileErrorCode.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun i18nKeyPrefixIsBlankSoDisplayTextFallsBackToDefault() {
        FileErrorCode.entries.forEach {
            assertEquals("", it.i18nKeyPrefix)
            assertEquals(it.defaultDisplayText, it.displayText)
        }
    }

    @Test
    fun getMessage_withoutPlaceholdersReturnsDisplayText() {
        assertEquals("File upload failed", FileErrorCode.FILE_UPLOAD_FAIL.getMessage())
    }

    @Test
    fun getMessage_ignoresExtraParamsWhenNoPlaceholders() {
        val msg = FileErrorCode.FILE_DELETE_FAIL.getMessage("a.txt", 42)
        assertEquals("File deletion failed", msg)
        assertTrue(msg.isNotBlank())
    }

}
