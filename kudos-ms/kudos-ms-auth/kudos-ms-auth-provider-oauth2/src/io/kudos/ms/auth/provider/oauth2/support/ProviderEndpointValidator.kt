package io.kudos.ms.auth.provider.oauth2.support

import java.net.InetAddress
import java.net.URI

/** Baseline SSRF guard for provider endpoints loaded from configuration. */
object ProviderEndpointValidator {

    fun requireSafeHttps(value: String, field: String): String {
        val uri = runCatching { URI(value) }
            .getOrElse { throw IllegalArgumentException("Invalid $field URI", it) }
        require(uri.scheme.equals("https", ignoreCase = true)) { "$field must use HTTPS" }
        val host = requireNotNull(uri.host) { "$field must contain a host" }
        require(!host.equals("localhost", ignoreCase = true)) { "$field must not target localhost" }
        if (isIpLiteral(host)) {
            val address = InetAddress.getByName(host)
            require(
                !address.isAnyLocalAddress && !address.isLoopbackAddress &&
                    !address.isLinkLocalAddress && !address.isSiteLocalAddress && !address.isMulticastAddress
            ) { "$field must not target a private or local address" }
        }
        return uri.toASCIIString()
    }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.all { it.isDigit() || it == '.' }
}
