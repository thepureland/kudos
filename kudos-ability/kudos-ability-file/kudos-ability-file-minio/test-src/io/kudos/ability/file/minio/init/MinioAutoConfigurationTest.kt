package io.kudos.ability.file.minio.init

import io.kudos.ability.file.minio.init.properties.MinioProperties
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the fail-fast behavior of [MinioAutoConfiguration.minioClient]:
 * the bundled yml deliberately ships no default credentials, so missing/blank
 * endpoint or AK/SK must abort assembly with a message naming the property,
 * instead of connecting to MinIO with empty credentials.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MinioAutoConfigurationTest {

    private val autoConfiguration = MinioAutoConfiguration()

    private fun properties(
        endpoint: String? = "http://127.0.0.1:9000",
        accessKey: String? = "test-access-key",
        secretKey: String? = "test-secret-key"
    ) = MinioProperties().also {
        it.endpoint = endpoint
        it.accessKey = accessKey
        it.secretKey = secretKey
    }

    @Test
    fun minioClientBuildsWhenAllRequiredPropertiesConfigured() {
        // Building a client is offline; no MinIO server is contacted here.
        assertNotNull(autoConfiguration.minioClient(properties()))
    }

    @Test
    fun minioClientFailsFastWhenAccessKeyMissing() {
        val e = assertFailsWith<IllegalStateException> {
            autoConfiguration.minioClient(properties(accessKey = null))
        }
        assertTrue(e.message!!.contains("kudos.ability.file.minio.access-key"))
    }

    @Test
    fun minioClientFailsFastWhenAccessKeyBlank() {
        val e = assertFailsWith<IllegalStateException> {
            autoConfiguration.minioClient(properties(accessKey = "   "))
        }
        assertTrue(e.message!!.contains("kudos.ability.file.minio.access-key"))
    }

    @Test
    fun minioClientFailsFastWhenSecretKeyMissing() {
        val e = assertFailsWith<IllegalStateException> {
            autoConfiguration.minioClient(properties(secretKey = null))
        }
        assertTrue(e.message!!.contains("kudos.ability.file.minio.secret-key"))
    }

    @Test
    fun minioClientFailsFastWhenSecretKeyBlank() {
        val e = assertFailsWith<IllegalStateException> {
            autoConfiguration.minioClient(properties(secretKey = ""))
        }
        assertTrue(e.message!!.contains("kudos.ability.file.minio.secret-key"))
    }

    @Test
    fun minioClientFailsFastWhenEndpointMissing() {
        val e = assertFailsWith<IllegalStateException> {
            autoConfiguration.minioClient(properties(endpoint = null))
        }
        assertTrue(e.message!!.contains("kudos.ability.file.minio.endpoint"))
    }

}
