package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import org.springframework.core.io.InputStreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [DownloadFileModel].
 *
 * Mirrors [DeleteFileModelTest] for the from() parsing rules (leading slash
 * required, bucket segment required, blank rejected) plus property defaults
 * and mutation.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DownloadFileModelTest {

    @Test
    fun defaults_areNull() {
        val m = DownloadFileModel<InputStreamSource>()
        assertNull(m.bucketName)
        assertNull(m.filePath)
        assertNull(m.authServerParam)
    }

    @Test
    fun properties_areMutable() {
        val auth = AccessKeyServerParam("ak", "sk")
        val m = DownloadFileModel<InputStreamSource>().apply {
            bucketName = "b"
            filePath = "/f.txt"
            authServerParam = auth
        }
        assertEquals("b", m.bucketName)
        assertEquals("/f.txt", m.filePath)
        assertSame(auth, m.authServerParam)
    }

    @Test
    fun from_standardLeadingSlash() {
        val m = DownloadFileModel.from("/my-bucket/dir/sub/a.txt")
        assertEquals("my-bucket", m.bucketName)
        assertEquals("/dir/sub/a.txt", m.filePath)
    }

    @Test
    fun from_singleFileUnderBucket() {
        val m = DownloadFileModel.from("/bucket/x.png")
        assertEquals("bucket", m.bucketName)
        assertEquals("/x.png", m.filePath)
    }

    @Test
    fun from_noLeadingSlash_rejected() {
        val e = assertFailsWith<IllegalArgumentException> {
            DownloadFileModel.from("bucket/dir/a.txt")
        }
        assertEquals(true, e.message!!.contains("must start with '/'"))
    }

    @Test
    fun from_blank_rejected() {
        assertFailsWith<IllegalArgumentException> { DownloadFileModel.from("") }
        assertFailsWith<IllegalArgumentException> { DownloadFileModel.from("   ") }
    }

    @Test
    fun from_missingBucket_rejected() {
        assertFailsWith<IllegalArgumentException> { DownloadFileModel.from("/") }
        assertFailsWith<IllegalArgumentException> { DownloadFileModel.from("//x") }
    }

}
