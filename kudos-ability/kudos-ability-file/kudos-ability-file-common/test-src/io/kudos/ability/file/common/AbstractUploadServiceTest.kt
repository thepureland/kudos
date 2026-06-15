package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [AbstractUploadService].
 *
 * Covers:
 *  - fileUpload template flow: dispatch directory -> saveFile -> assemble result (filePath/pathPrefix)
 *  - dispatchFileDir default strategy branches: tenantId present/blank/null x category present/blank/null,
 *    falling back to year/month/day bucketing
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AbstractUploadServiceTest {

    private class FakeUploadService(private val savedPath: String? = "saved/path.txt") : AbstractUploadService() {
        var receivedFileDir: String? = null

        override fun saveFile(model: UploadFileModel<*>, fileDir: String): String? {
            receivedFileDir = fileDir
            return savedPath
        }

        override fun pathPrefix(): String = "/var/file/upload/"

        // expose protected method for direct branch testing
        fun dispatchDir(model: UploadFileModel<*>): String = dispatchFileDir(model)
    }

    private fun model(tenantId: String? = null, category: String? = null) =
        UploadFileModel<Nothing>().apply {
            this.tenantId = tenantId
            this.category = category
        }

    private fun todayDir(): String {
        val today = LocalDate.now()
        return "${today.year}/${today.monthValue}/${today.dayOfMonth}"
    }

    @Test
    fun fileUpload_assemblesResultFromSaveFileAndPathPrefix() {
        val service = FakeUploadService()
        val result = service.fileUpload(model(tenantId = "t1", category = "avatar"))
        assertEquals("saved/path.txt", result.filePath)
        assertEquals("/var/file/upload/", result.pathPrefix)
        assertEquals("t1/avatar", service.receivedFileDir)
    }

    @Test
    fun fileUpload_keepsNullFilePathWhenSaveFileReturnsNull() {
        val service = FakeUploadService(savedPath = null)
        val result = service.fileUpload(model(category = "c"))
        assertNull(result.filePath)
        assertEquals("/var/file/upload/", result.pathPrefix)
    }

    @Test
    fun dispatchFileDir_tenantAndCategory() {
        assertEquals("tenant-9/img", FakeUploadService().dispatchDir(model("tenant-9", "img")))
    }

    @Test
    fun dispatchFileDir_categoryOnly() {
        assertEquals("docs", FakeUploadService().dispatchDir(model(null, "docs")))
    }

    @Test
    fun dispatchFileDir_tenantOnly_fallsBackToDateBuckets() {
        assertEquals("t1/${todayDir()}", FakeUploadService().dispatchDir(model("t1", null)))
    }

    @Test
    fun dispatchFileDir_noTenantNoCategory_usesDateBuckets() {
        assertEquals(todayDir(), FakeUploadService().dispatchDir(model(null, null)))
    }

    @Test
    fun dispatchFileDir_blankTenantAndBlankCategory_treatedAsAbsent() {
        assertEquals(todayDir(), FakeUploadService().dispatchDir(model("   ", "")))
    }

    @Test
    fun dispatchFileDir_blankCategoryWithTenant() {
        assertEquals("t2/${todayDir()}", FakeUploadService().dispatchDir(model("t2", "  ")))
    }

}
