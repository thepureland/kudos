package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.minio.init.properties.MinioProperties
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure-logic tests for [AccessKeyMinioClientBuilder]: assembling a [io.minio.MinioClient]
 * from endpoint + AK/SK is fully offline (no MinIO server is contacted), so both the
 * happy path and the fail-fast branches for missing configuration can be verified
 * without any container.
 *
 * Covers:
 * - build() succeeds when both minioProperties and authServerParam are injected.
 * - build() fails fast with a clear message when minioProperties was never injected.
 * - build() fails fast with a clear message when authServerParam was never injected.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AccessKeyMinioClientBuilderTest {

    private fun properties() = MinioProperties().also {
        it.endpoint = "http://127.0.0.1:9000"
    }

    @Test
    fun buildSucceedsWithEndpointAndCredentials() {
        val builder = AccessKeyMinioClientBuilder()
        builder.setMinioProperties(properties())
        builder.setAuthServerParam(AccessKeyServerParam("ak", "sk"))

        // Building a client is offline; no MinIO server is contacted here.
        assertNotNull(builder.build())
    }

    @Test
    fun buildFailsFastWhenMinioPropertiesMissing() {
        val builder = AccessKeyMinioClientBuilder()
        builder.setAuthServerParam(AccessKeyServerParam("ak", "sk"))

        val e = assertFailsWith<IllegalArgumentException> { builder.build() }
        assertTrue(e.message!!.contains("minioProperties"))
    }

    @Test
    fun buildFailsFastWhenAuthServerParamMissing() {
        val builder = AccessKeyMinioClientBuilder()
        builder.setMinioProperties(properties())

        val e = assertFailsWith<IllegalArgumentException> { builder.build() }
        assertTrue(e.message!!.contains("authServerParam"))
    }

}
