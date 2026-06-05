package io.kudos.ability.file.common.compress.utils

import kotlin.test.*

/**
 * test for CompressUtil
 *
 * @author K
 * @since 1.0.0
 */
internal class CompressUtilTest {

    @Test
    fun validExtensionAcceptsAllowlist() {
        assertTrue(CompressUtil.validExtension("jpg"))
        assertTrue(CompressUtil.validExtension("jpeg"))
        assertTrue(CompressUtil.validExtension("png"))
    }

    @Test
    fun validExtensionIsCaseInsensitive() {
        assertTrue(CompressUtil.validExtension("JPG"))
        assertTrue(CompressUtil.validExtension("Png"))
        assertTrue(CompressUtil.validExtension("JPEG"))
    }

    @Test
    fun validExtensionRejectsOthersAndBlank() {
        assertFalse(CompressUtil.validExtension("gif"))
        assertFalse(CompressUtil.validExtension("bmp"))
        assertFalse(CompressUtil.validExtension("webp"))
        assertFalse(CompressUtil.validExtension(""))
        assertFalse(CompressUtil.validExtension("   "))
    }

    @Test
    fun isPicResolvesBySuffix() {
        assertTrue(CompressUtil.isPic("photo.jpg"))
        assertTrue(CompressUtil.isPic("a/b/c/avatar.PNG"))
        assertTrue(CompressUtil.isPic("scan.jpeg"))

        assertFalse(CompressUtil.isPic("doc.pdf"))
        assertFalse(CompressUtil.isPic("archive.zip"))
        assertFalse(CompressUtil.isPic("noextension"))
    }
}
