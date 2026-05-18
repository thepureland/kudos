package io.kudos.ability.file.common.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [UploadContentTypeEnum] 回归测试。
 *
 * 历史 bug：`BMP("pdf", "application/x-bmp")` 后缀写错，PDF 被打成 BMP 的 contentType，
 * BMP 文件本身查不到——下面两个断言锁定修复后的行为，防止有人改回去。
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
