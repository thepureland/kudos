package io.kudos.ability.file.common.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [DeleteFileModel.from] 回归测试。
 *
 * 历史 bug：旧实现对无前导 `/` 的 `fullPath` 会把首段当 bucket、后段当 path 错位一格；
 * 修复后无前导 `/` 直接拒绝。
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
        // 修复前：返回 bucketName="dir"，与调用方意图不符——现在显式拒绝
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
        // "/" 切完只有一个空段
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("/") }
        // "//x" 切完 bucket 段是空串
        assertFailsWith<IllegalArgumentException> { DeleteFileModel.from("//x") }
    }
}
