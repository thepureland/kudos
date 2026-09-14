package io.kudos.ms.tag.core.catalog.cache

import io.kudos.ms.tag.core.rule.model.TagRuleView
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Keeps immutable rule versions separate from the deliberately short-lived published pointer. */
@Component
open class PublishedRuleCache(
    private val clock: Clock = Clock.systemUTC(),
    private val pointerTtl: Duration = Duration.ofSeconds(30),
) {
    private val versions = ConcurrentHashMap<PublishedRuleVersionKey, TagRuleView>()
    private val currentPointers = ConcurrentHashMap<PublishedRulePointerKey, ExpiringPointer>()

    init {
        require(!pointerTtl.isNegative && !pointerTtl.isZero) { "Published-rule pointer TTL must be positive." }
    }

    open fun getVersion(
        tenantId: String,
        ruleId: String,
        ruleVersion: Long,
        loader: (String, String, Long) -> TagRuleView?,
    ): TagRuleView? {
        val key = PublishedRuleVersionKey(tenantId, ruleId, ruleVersion)
        versions[key]?.let { return it }
        val loaded = loader(tenantId, ruleId, ruleVersion) ?: return null
        require(loaded.tenantId == tenantId && loaded.id == ruleId && loaded.ruleVersion == ruleVersion) {
            "Rule loader must return the exact requested tenant, rule ID, and version."
        }
        return versions.putIfAbsent(key, loaded) ?: loaded
    }

    open fun putVersion(rule: TagRuleView): TagRuleView {
        val key = PublishedRuleVersionKey(rule.tenantId, rule.id, rule.ruleVersion)
        return versions.putIfAbsent(key, rule) ?: rule
    }

    open fun currentPointer(
        tenantId: String,
        tagCode: String,
        loader: (String, String) -> PublishedRulePointer?,
    ): PublishedRulePointer? {
        val key = PublishedRulePointerKey(tenantId, tagCode)
        val now = clock.instant()
        currentPointers[key]?.takeIf { now.isBefore(it.expiresAt) }?.let { return it.pointer }
        currentPointers.remove(key)
        val loaded = loader(tenantId, tagCode) ?: return null
        updateCurrentPointer(tenantId, tagCode, loaded)
        return loaded
    }

    open fun updateCurrentPointer(tenantId: String, tagCode: String, pointer: PublishedRulePointer) {
        val key = PublishedRulePointerKey(tenantId, tagCode)
        currentPointers[key] = ExpiringPointer(pointer, clock.instant().plus(pointerTtl))
    }

    open fun invalidateCurrentPointer(tenantId: String, tagCode: String) {
        currentPointers.remove(PublishedRulePointerKey(tenantId, tagCode))
    }

    private data class ExpiringPointer(val pointer: PublishedRulePointer, val expiresAt: Instant)
}

data class PublishedRulePointer(val ruleId: String, val ruleVersion: Long) {
    init {
        require(ruleId.isNotBlank()) { "Published rule ID must not be blank." }
        require(ruleVersion > 0) { "Published rule version must be positive." }
    }
}

data class PublishedRuleVersionKey(val tenantId: String, val ruleId: String, val ruleVersion: Long) {
    init {
        require(tenantId.isNotBlank()) { "Published rule tenant must not be blank." }
        require(ruleId.isNotBlank()) { "Published rule ID must not be blank." }
        require(ruleVersion > 0) { "Published rule version must be positive." }
    }
}

data class PublishedRulePointerKey(val tenantId: String, val tagCode: String) {
    init {
        require(tenantId.isNotBlank()) { "Published rule tenant must not be blank." }
        require(tagCode.isNotBlank()) { "Published rule tag code must not be blank." }
    }
}
