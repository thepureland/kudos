package io.kudos.ability.file.minio

import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.base.error.ServiceException
import org.springframework.core.io.InputStreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pure-logic tests for [MinioUploadService.resolveFileName]: path-traversal characters
 * in `fileName` must be rejected (same rule as the file-local implementation), because
 * the business side concatenates `publicEndpoint + filePath` into URLs where a `..`
 * segment may be normalized by browsers/gateways/CDNs into another object's URL.
 *
 * No Spring context / MinIO container needed - the method only reads the model.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MinioUploadServiceResolveFileNameTest {

    private val service = MinioUploadService()

    private fun model(fileName: String?, fileSuffix: String? = "txt") =
        UploadFileModel<InputStreamSource>().also {
            it.fileName = fileName
            it.fileSuffix = fileSuffix
        }

    @Test
    fun validFileNameIsReturnedAsIs() {
        assertEquals("report.pdf", service.resolveFileName(model("report.pdf")))
    }

    @Test
    fun missingFileNameGeneratesUuidBasedName() {
        val name = service.resolveFileName(model(null))
        assertTrue(name.endsWith(".txt"))
        assertTrue(name.length > ".txt".length)
    }

    @Test
    fun blankFileNameGeneratesUuidBasedName() {
        val name = service.resolveFileName(model("   "))
        assertTrue(name.endsWith(".txt"))
    }

    @Test
    fun dottedSuffixDoesNotProduceDoubleDot() {
        val name = service.resolveFileName(model(null, ".txt"))
        assertTrue(name.endsWith(".txt"))
        assertTrue(!name.contains(".."))
    }

    @Test
    fun missingSuffixGeneratesBareUuid() {
        val name = service.resolveFileName(model(null, null))
        assertTrue(!name.contains('.'))
        assertTrue(name.isNotBlank())
    }

    @Test
    fun suffixWithTraversalCharactersIsRejected() {
        val e = assertFailsWith<ServiceException> {
            service.resolveFileName(model(null, "txt/evil"))
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, e.errorCode)
    }

    @Test
    fun fileNameWithParentTraversalIsRejected() {
        val e = assertFailsWith<ServiceException> {
            service.resolveFileName(model("../../etc/passwd"))
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, e.errorCode)
    }

    @Test
    fun fileNameWithDotDotOnlyIsRejected() {
        val e = assertFailsWith<ServiceException> {
            service.resolveFileName(model("a..b.txt"))
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, e.errorCode)
    }

    @Test
    fun fileNameWithForwardSlashIsRejected() {
        val e = assertFailsWith<ServiceException> {
            service.resolveFileName(model("dir/name.txt"))
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, e.errorCode)
    }

    @Test
    fun fileNameWithBackslashIsRejected() {
        val e = assertFailsWith<ServiceException> {
            service.resolveFileName(model("dir\\name.txt"))
        }
        assertEquals(FileErrorCode.FILE_UPLOAD_FAIL, e.errorCode)
    }

}
