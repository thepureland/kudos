package io.kudos.ability.file.common.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for [UploadContentTypeEnum].
 *
 * Historical bug: `BMP("pdf", "application/x-bmp")` had the wrong suffix, causing PDF
 * to be tagged with BMP's contentType, while actual BMP files were not found —
 * the two assertions below lock in the post-fix behavior so the bug cannot be
 * reintroduced.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class UploadContentTypeEnumTest {

    @Test
    fun pdfReturnsPdfContentType() {
        val e = UploadContentTypeEnum.enumOf("pdf")
        assertEquals(UploadContentTypeEnum.PDF, e)
        assertEquals("application/pdf", e.contentType)
    }

    @Test
    fun bmpReturnsBmpContentType() {
        val e = UploadContentTypeEnum.enumOf("bmp")
        assertEquals(UploadContentTypeEnum.BMP, e)
        assertEquals("image/bmp", e.contentType)
    }

    @Test
    fun caseInsensitive() {
        assertEquals(UploadContentTypeEnum.PNG, UploadContentTypeEnum.enumOf("PNG"))
        assertEquals(UploadContentTypeEnum.JPEG, UploadContentTypeEnum.enumOf("JPEG"))
    }

    @Test
    fun unknownReturnsDefault() {
        assertEquals(UploadContentTypeEnum.DEFAULT, UploadContentTypeEnum.enumOf("xyz"))
        assertEquals(UploadContentTypeEnum.DEFAULT, UploadContentTypeEnum.enumOf(""))
    }
}
