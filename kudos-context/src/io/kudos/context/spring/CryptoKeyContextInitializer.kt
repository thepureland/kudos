package io.kudos.context.spring

import io.kudos.base.security.CryptoKey
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered

/** Installs external encryption key material before application beans decrypt persisted secrets. */
class CryptoKeyContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val env = applicationContext.environment
        val supplied = env.getProperty("kudos.crypto.default-key")
            ?: env.getProperty("KUDOS_CRYPTO_DEFAULT_KEY")
        if (supplied != null) CryptoKey.configureDefaultKey(supplied)
        // Fail on all deployments, including ones named staging/live rather than prod.
        // A programmatically installed key is supported; missing configuration never selects a public key.
        CryptoKey.KEY_DEFAULT
    }
}
