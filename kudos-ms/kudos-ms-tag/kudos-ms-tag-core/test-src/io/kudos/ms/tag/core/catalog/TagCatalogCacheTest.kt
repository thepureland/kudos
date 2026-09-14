package io.kudos.ms.tag.core.catalog

import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.core.catalog.cache.PublishedRuleCache
import io.kudos.ms.tag.core.catalog.cache.PublishedRulePointer
import io.kudos.ms.tag.core.catalog.cache.TagCatalogCache
import io.kudos.ms.tag.core.catalog.cache.TagCatalogCacheKey
import io.kudos.ms.tag.core.catalog.cache.TagCatalogEntryType
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.model.TagRuleView
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

internal class TagCatalogCacheTest {

    @Test
    fun `catalog cache is isolated by tenant stable identity and configuration version`() {
        val cache = TagCatalogCache()
        var loads = 0
        fun load(key: TagCatalogCacheKey): String = cache.getOrLoad(key, String::class) {
            "value-${++loads}"
        }!!

        val base = TagCatalogCacheKey("tenant-a", TagCatalogEntryType.TAG_DEFINITION, "large-house", 3)
        val first = load(base)

        assertSame(first, load(base))
        assertNotEquals(first, load(base.copy(tenantId = "tenant-b")))
        assertNotEquals(first, load(base.copy(stableIdOrCode = "villa")))
        assertNotEquals(first, load(base.copy(configurationVersion = 4)))
        assertEquals(4, loads)
    }

    @Test
    fun `published pointer can move while immutable compiled versions stay addressable`() {
        val cache = PublishedRuleCache(Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC), Duration.ofSeconds(30))
        var versionLoads = 0
        val v1 = rule("rule-1", 1)
        val v2 = rule("rule-2", 2)

        assertSame(v1, cache.getVersion("tenant-a", "rule-1", 1) { _, _, _ -> versionLoads++; v1 })
        cache.updateCurrentPointer("tenant-a", "large-house", PublishedRulePointer("rule-1", 1))
        assertEquals(PublishedRulePointer("rule-1", 1), cache.currentPointer("tenant-a", "large-house") { _, _ -> null })

        cache.updateCurrentPointer("tenant-a", "large-house", PublishedRulePointer("rule-2", 2))
        assertSame(v2, cache.getVersion("tenant-a", "rule-2", 2) { _, _, _ -> versionLoads++; v2 })

        assertEquals(PublishedRulePointer("rule-2", 2), cache.currentPointer("tenant-a", "large-house") { _, _ -> null })
        assertSame(v1, cache.getVersion("tenant-a", "rule-1", 1) { _, _, _ -> error("v1 must remain cached") })
        assertEquals(2, versionLoads)
    }

    @Test
    fun `exact version miss never substitutes the current version`() {
        val cache = PublishedRuleCache()
        cache.updateCurrentPointer("tenant-a", "large-house", PublishedRulePointer("rule-2", 2))

        val requested = mutableListOf<Triple<String, String, Long>>()
        val result = cache.getVersion("tenant-a", "rule-1", 1) { tenant, ruleId, version ->
            requested += Triple(tenant, ruleId, version)
            null
        }

        assertEquals(null, result)
        assertEquals(listOf(Triple("tenant-a", "rule-1", 1L)), requested)
    }

    private fun rule(id: String, version: Long) = TagRuleView(
        id = id,
        tenantId = "tenant-a",
        tagId = "tag-1",
        tagCode = "large-house",
        subjectType = "estate.house",
        ruleVersion = version,
        status = TagRuleStatus.PUBLISHED,
        expression = TagRuleExpression.HasTag("seed"),
        checksum = version.toString().repeat(64).take(64),
    )
}
