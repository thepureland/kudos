package io.kudos.ms.auth.provider.oauth2.service

/** Resolves ordered dot-separated claim paths without evaluating expressions or arbitrary code. */
internal object ExternalClaimResolver {

    fun first(claims: Map<String, Any?>, paths: Collection<String>): Any? =
        paths.asSequence().mapNotNull { resolve(claims, it) }.firstOrNull()

    fun firstString(claims: Map<String, Any?>, paths: Collection<String>): String? =
        first(claims, paths)?.let { value ->
            when (value) {
                is String -> value.trim().takeIf { it.isNotBlank() }
                is Number, is Boolean -> value.toString()
                else -> null
            }
        }

    private fun resolve(claims: Map<String, Any?>, path: String): Any? {
        var current: Any? = claims
        path.split('.').forEach { segment ->
            current = when (val value = current) {
                is Map<*, *> -> value[segment]
                is List<*> -> segment.toIntOrNull()?.let(value::getOrNull)
                else -> return null
            }
        }
        return current
    }
}
