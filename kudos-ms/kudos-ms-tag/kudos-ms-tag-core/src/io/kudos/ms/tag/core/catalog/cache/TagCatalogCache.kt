package io.kudos.ms.tag.core.catalog.cache

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/** Configuration-only cache. Runtime facts, memberships, assignments, jobs, and query results do not belong here. */
@Component
open class TagCatalogCache {
    private val entries = ConcurrentHashMap<TagCatalogCacheKey, Any>()

    open fun <T : Any> getOrLoad(key: TagCatalogCacheKey, type: KClass<T>, loader: () -> T?): T? {
        entries[key]?.let { return type.castValue(key, it) }
        val loaded = loader() ?: return null
        require(type.isInstance(loaded)) { "Catalog loader returned ${loaded::class.qualifiedName}, expected ${type.qualifiedName}." }
        return type.castValue(key, entries.putIfAbsent(key, loaded) ?: loaded)
    }

    open fun invalidate(key: TagCatalogCacheKey) {
        entries.remove(key)
    }

    open fun invalidateTenant(tenantId: String) {
        entries.keys.removeIf { it.tenantId == tenantId }
    }

    private fun <T : Any> KClass<T>.castValue(key: TagCatalogCacheKey, value: Any): T {
        require(isInstance(value)) { "Catalog cache key [$key] was reused with an incompatible value type." }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}

enum class TagCatalogEntryType {
    TAG_DEFINITION,
    TAG_SET,
    ATTRIBUTE_DEFINITION,
    TAXONOMY,
    RULE_DEPENDENCY,
}

data class TagCatalogCacheKey(
    val tenantId: String,
    val entryType: TagCatalogEntryType,
    val stableIdOrCode: String,
    val configurationVersion: Long,
) {
    init {
        require(tenantId.isNotBlank()) { "Catalog cache tenant must not be blank." }
        require(stableIdOrCode.isNotBlank()) { "Catalog cache identity must not be blank." }
        require(configurationVersion >= 0) { "Catalog configuration version must not be negative." }
    }
}
