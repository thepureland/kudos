package io.kudos.ability.distributed.discovery.nacos.loadbalancer

import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [HintZoneServiceInstanceListSupplier.filteredByHint] 的纯函数单测。
 *
 * 三大分支：
 *  - **hint 非空，有命中** → 只返回命中实例
 *  - **hint 非空，无命中** → 降级返回全部实例（避免业务被完全拒绝）
 *  - **hint 为空 + 默认 zone 非空** → 只返回 zone 与默认一致或未设 zone 的实例
 *  - **hint 为空 + 默认 zone 为空** → 原样返回
 *
 * 同时验证可配置的 [HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY] 替代场景：
 * metadata 字段名换成 `region` 也能命中。
 */
internal class HintZoneServiceInstanceListSupplierTest {

    private val east = instance("svc-east", "east")
    private val west = instance("svc-west", "west")
    private val none = instance("svc-none", null)

    @Test
    fun hintHit_returnsOnlyMatching() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east), result)
    }

    @Test
    fun hintMiss_returnsAllAsFallback() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "moon", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        // 命中为空 → 降级返回原列表（按引用，确保是 fast-path）
        assertSame(all, result)
    }

    @Test
    fun noHint_defaultZoneSet_returnsMatchingPlusUntagged() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "east",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east, none), result, "应包含与默认 zone 一致 + 未设 zone 的实例")
    }

    @Test
    fun noHint_blankDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "无默认 zone 时应原样返回")
    }

    @Test
    fun noHint_nullDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result)
    }

    @Test
    fun blankHint_treatedAsAbsent() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "   ", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "空白字符串应当与 null 同义")
    }

    @Test
    fun customMetadataKey_works() {
        val east = DefaultServiceInstance("svc-east", "svc", "h1", 80, false, mapOf("region" to "east"))
        val west = DefaultServiceInstance("svc-west", "svc", "h2", 80, false, mapOf("region" to "west"))
        val all = mutableListOf<ServiceInstance>(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = "region",
        )
        assertEquals(1, result.size)
        assertTrue(result.contains(east))
    }

    private fun instance(id: String, zone: String?): ServiceInstance {
        val metadata = if (zone == null) emptyMap() else mapOf("zone" to zone)
        return DefaultServiceInstance(id, "svc", "host-$id", 80, false, metadata)
    }
}
