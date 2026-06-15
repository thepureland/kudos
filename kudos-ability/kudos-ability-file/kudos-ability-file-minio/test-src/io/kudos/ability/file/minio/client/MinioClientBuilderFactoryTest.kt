package io.kudos.ability.file.minio.client

import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pure-logic tests for [MinioClientBuilderFactory]: the `when` dispatch over the
 * [AuthServerParam] subtype must return a fully-initialized builder for both known
 * authentication forms, and null for unknown subtypes (the caller decides whether
 * to fall back or throw).
 *
 * The factory's @Autowired fields are injected via reflection — no Spring context
 * is needed to exercise the dispatch logic.
 *
 * Covers:
 * - AccessKeyServerParam  -> AccessKeyMinioClientBuilder, ready to build().
 * - AccessTokenServerParam -> AccessTokenMinioClientBuilder, ready to build().
 * - Unknown subtype -> null.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MinioClientBuilderFactoryTest {

    private fun factory(): MinioClientBuilderFactory {
        val factory = MinioClientBuilderFactory()
        inject(factory, "minioProperties", MinioProperties().also { it.endpoint = "http://127.0.0.1:9000" })
        inject(factory, "accessTokenServer", AccessTokenServerProperties())
        return factory
    }

    private fun inject(target: Any, fieldName: String, value: Any) {
        target.javaClass.getDeclaredField(fieldName).also { it.isAccessible = true }.set(target, value)
    }

    @Test
    fun accessKeyParamYieldsFullyInitializedAccessKeyBuilder() {
        val builder = factory().getInstance(AccessKeyServerParam("ak", "sk"))

        assertIs<AccessKeyMinioClientBuilder>(builder)
        // Fully initialized: build() must succeed offline (would fail fast if props/param were missing).
        assertNotNull(builder.build())
    }

    @Test
    fun accessTokenParamYieldsFullyInitializedAccessTokenBuilder() {
        val param = AccessTokenServerParam().also { it.headerValue = "Bearer t" }

        val builder = factory().getInstance(param)

        assertIs<AccessTokenMinioClientBuilder>(builder)
        // Fully initialized: build() is offline (the credentials provider fetches lazily).
        assertNotNull(builder.build())
    }

    @Test
    fun unknownAuthParamTypeYieldsNull() {
        val unknown = object : AuthServerParam {}

        assertNull(factory().getInstance(unknown))
    }

}
