package io.kudos.ability.file.minio

import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.base.error.ServiceException
import org.springframework.core.io.InputStreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pure-logic tests for the three MinIO services' "dynamic client" selection when the
 * [AuthServerParam] subtype is unknown to [MinioClientBuilderFactory] (factory returns
 * null -> `requireNotNull` throws -> each service maps it to its own error code).
 *
 * Everything is offline: the factory's `when` falls into the `else -> null` branch
 * before any lateinit field or network access is touched, so the services only need
 * the factory injected via reflection.
 *
 * Covers:
 * - MinioUploadService.saveFile generic catch -> FILE_ACCESS_ERROR.
 * - MinioDownLoadService.readFileToByte / readFileToStream generic catch -> FILE_ACCESS_ERROR.
 * - MinioDeleteService.delete generic catch -> FILE_DELETE_FAIL.
 * - MinioUploadService.pathPrefix fail-fast when publicEndpoint is not configured,
 *   and pass-through when it is.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MinioServicesUnknownAuthTest {

    /** An auth param subtype the factory does not know about. */
    private class UnknownAuthParam : AuthServerParam

    private fun inject(target: Any, fieldName: String, value: Any) {
        target.javaClass.getDeclaredField(fieldName).also { it.isAccessible = true }.set(target, value)
    }

    @Test
    fun uploadWithUnknownAuthTypeFailsWithAccessError() {
        val service = MinioUploadService()
        inject(service, "minioClientBuilderFactory", MinioClientBuilderFactory())
        val model = UploadFileModel<InputStreamSource>().also {
            it.bucketName = "docs"
            it.fileName = "a.txt"
            it.authServerParam = UnknownAuthParam()
        }

        val e = assertFailsWith<ServiceException> { service.fileUpload(model) }
        assertEquals(FileErrorCode.FILE_ACCESS_ERROR, e.errorCode)
    }

    @Test
    fun downloadWithUnknownAuthTypeFailsWithAccessError() {
        val service = MinioDownLoadService()
        inject(service, "minioClientBuilderFactory", MinioClientBuilderFactory())
        val model = DownloadFileModel<InputStreamSource>().also {
            it.bucketName = "docs"
            it.filePath = "a.txt"
            it.authServerParam = UnknownAuthParam()
        }

        val e = assertFailsWith<ServiceException> { service.download(model) }
        assertEquals(FileErrorCode.FILE_ACCESS_ERROR, e.errorCode)
    }

    @Test
    fun downloadStreamWithUnknownAuthTypeFailsWithAccessError() {
        val service = MinioDownLoadService()
        inject(service, "minioClientBuilderFactory", MinioClientBuilderFactory())
        val model = DownloadFileModel<InputStreamSource>().also {
            it.bucketName = "docs"
            it.filePath = "a.txt"
            it.authServerParam = UnknownAuthParam()
        }

        val e = assertFailsWith<ServiceException> { service.downloadStream(model) }
        assertEquals(FileErrorCode.FILE_ACCESS_ERROR, e.errorCode)
    }

    @Test
    fun deleteWithUnknownAuthTypeFailsWithDeleteFail() {
        val service = MinioDeleteService()
        inject(service, "minioClientBuilderFactory", MinioClientBuilderFactory())
        val model = DeleteFileModel().also {
            it.bucketName = "docs"
            it.filePath = "a.txt"
            it.authServerParam = UnknownAuthParam()
        }

        val e = assertFailsWith<ServiceException> { service.delete(model) }
        assertEquals(FileErrorCode.FILE_DELETE_FAIL, e.errorCode)
    }

    @Test
    fun pathPrefixFailsFastWhenPublicEndpointMissing() {
        val service = MinioUploadService()
        inject(service, "properties", MinioProperties()) // publicEndpoint left null

        val e = assertFailsWith<IllegalArgumentException> { service.pathPrefix() }
        assertTrue(e.message!!.contains("publicEndpoint"))
    }

    @Test
    fun pathPrefixReturnsConfiguredPublicEndpoint() {
        val service = MinioUploadService()
        inject(service, "properties", MinioProperties().also { it.publicEndpoint = "https://cdn.example.com" })

        assertEquals("https://cdn.example.com", service.pathPrefix())
    }

}
