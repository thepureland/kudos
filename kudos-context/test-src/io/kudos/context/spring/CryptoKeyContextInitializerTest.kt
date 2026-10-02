package io.kudos.context.spring

import io.kudos.base.security.CryptoKey
import org.springframework.context.support.StaticApplicationContext
import org.springframework.core.env.MapPropertySource
import kotlin.test.*

internal class CryptoKeyContextInitializerTest {
    @Test fun installsExternalKeyBeforeRefresh() {
        val original = CryptoKey.KEY_DEFAULT
        val context = StaticApplicationContext()
        try {
            val supplied = "startup-test-key-0123456789-abcdefghijklmnopqrstuvwxyz"
            context.environment.propertySources.addFirst(MapPropertySource("test", mapOf("kudos.crypto.default-key" to supplied)))
            CryptoKeyContextInitializer().initialize(context)
            assertEquals(supplied, CryptoKey.KEY_DEFAULT)
        } finally {
            CryptoKey.configureDefaultKey(original)
            context.close()
        }
    }

    @Test fun rejectsWeakExternalConfiguration() {
        val context = StaticApplicationContext()
        context.environment.propertySources.addFirst(MapPropertySource("test", mapOf("kudos.crypto.default-key" to "weak")))
        try {
            assertFailsWith<IllegalArgumentException> { CryptoKeyContextInitializer().initialize(context) }
        } finally { context.close() }
    }
}
