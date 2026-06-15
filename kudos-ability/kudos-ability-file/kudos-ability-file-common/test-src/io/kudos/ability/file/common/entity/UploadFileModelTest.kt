package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import org.springframework.core.io.ByteArrayResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [UploadFileModel].
 *
 * Covers:
 *  - default values of all properties (null fields + non-compressing default CompressionConfig)
 *  - setter/getter round-trips for every property, including the generic [UploadFileModel.inputStreamSource]
 *    and the [io.kudos.ability.file.common.auth.AuthServerParam] reference
 *  - Unicode / Windows-style values are stored verbatim (the model performs no normalization)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class UploadFileModelTest {

    @Test
    fun defaults_allNullAndCompressionDisabled() {
        val model = UploadFileModel<ByteArrayResource>()
        assertNull(model.bucketName)
        assertNull(model.tenantId)
        assertNull(model.category)
        assertNull(model.fileSuffix)
        assertNull(model.inputStreamSource)
        assertNull(model.authServerParam)
        assertNull(model.fileName)
        assertFalse(model.compressionConfig.enabled)
        assertFalse(model.compressionConfig.webp)
    }

    @Test
    fun properties_roundTrip() {
        val source = ByteArrayResource(byteArrayOf(1, 2, 3))
        val auth = AccessKeyServerParam("ak", "sk")
        val model = UploadFileModel<ByteArrayResource>().apply {
            bucketName = "my-bucket"
            tenantId = "t-001"
            category = "avatar"
            fileSuffix = "jpg"
            inputStreamSource = source
            authServerParam = auth
            fileName = "photo.jpg"
        }
        assertEquals("my-bucket", model.bucketName)
        assertEquals("t-001", model.tenantId)
        assertEquals("avatar", model.category)
        assertEquals("jpg", model.fileSuffix)
        assertSame(source, model.inputStreamSource)
        assertSame(auth, model.authServerParam)
        assertEquals("photo.jpg", model.fileName)
    }

    @Test
    fun properties_storeUnicodeAndWindowsPathVerbatim(): Unit {
        val model = UploadFileModel<ByteArrayResource>().apply {
            bucketName = "桶-名稱"
            fileName = "圖 片.png"
            category = "C:\\Windows\\Style"
        }
        assertEquals("桶-名稱", model.bucketName)
        assertEquals("圖 片.png", model.fileName)
        assertEquals("C:\\Windows\\Style", model.category)
    }

    @Test
    fun compressionConfig_isReplaceable() {
        val model = UploadFileModel<ByteArrayResource>()
        val config = io.kudos.ability.file.common.compress.support.CompressionConfig().apply {
            enabled = true
            webp = true
        }
        model.compressionConfig = config
        assertSame(config, model.compressionConfig)
    }

}
