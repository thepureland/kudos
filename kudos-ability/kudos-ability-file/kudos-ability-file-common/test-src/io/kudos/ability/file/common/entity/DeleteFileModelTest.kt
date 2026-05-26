package io.kudos.ability.file.common.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Regression tests for [DeleteFileModel.from].
 *
 * Historical bug: the old implementation, when given a `fullPath` without a leading
 * `/`, would treat the first segment as the bucket and the rest as the path (off by
 * one). After the fix, paths without a leading `/` are rejected outright.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class DeleteFileModelTest {

    @Test
    fun from_standardLeadingSlash() {
        val m = DeleteFileModel.from("/my-bucket/dir/a.txt")
        assertEquals("my-bucket", m.bucketName)
        assertEquals("/dir/a.txt", m.filePath)
    }

    @Test
    fun from_singleFileUnderBucket() {
        val m = DeleteFileModel.from("/bucket/x.png")
        assertEquals("bucket", m.bucketName)
        assertEquals("/x.png", m.filePath)
    }

    @Test
    fun from_noLeadingSlash_rejected() {
        // Before fix: returned bucketName="dir", which does not match caller intent — now explicitly rejected.
        assertFailsWith<IllegalArgumentException> {
            DeleteFileModel.from("bucket/dir/a.txt")
        }
    }

    @Test
    fun from_blank_rejected() {
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("") }
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("   ") }
    }

    @Test
    fun from_missingBucket_rejected() {
        // Splitting "/" produces a single empty segment.
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("/") }
        // Splitting "//x" leaves the bucket segment as an empty string.
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("//x") }
    }
}
