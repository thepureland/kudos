package io.kudos.ability.distributed.config.nacos

import com.alibaba.cloud.nacos.NacosPropertySourceRepository
import com.alibaba.cloud.nacos.client.NacosPropertySource
import io.kudos.ability.distributed.config.nacos.decrypt.NacosConfigValueDecryptor
import io.kudos.context.config.IConfigDataFinder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import java.util.Date
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [NacosConfigDataFinder] 单测——**不启动 Nacos**，直接往
 * [NacosPropertySourceRepository] 的静态 map 注入测试数据。
 *
 * 覆盖：
 *  - 按 `dataId` 命中：finder 返回对应 PropertySource
 *  - 没命中：返回 null
 *  - 同时挂多份 nacos source：按 dataId 匹配的那一份胜出
 *  - **SPI 注册**：`ServiceLoader.load(IConfigDataFinder::class.java)` 能找到 [NacosConfigDataFinder]
 *    （守护 `resources/META-INF/services/...` 文件没被误删）
 *
 * `NacosPropertySourceRepository` 的静态 map 在整个 JVM 内共享、无 clear API——本测试
 * 用反射在每个用例前后清理，避免泄漏到同一 JVM 的其他测试。
 */
internal class NacosConfigDataFinderTest {

    private val finder = NacosConfigDataFinder()

    @BeforeTest
    fun clearRepoBefore() = clearRepoViaReflection()

    @AfterTest
    fun clearRepoAfter() = clearRepoViaReflection()

    @Test
    fun findConfigData_returnsRegisteredSource_byDataId() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))

        val source = finder.findConfigData("app.yaml")

        assertNotNull(source)
        assertEquals("v", source.getProperty("k"))
    }

    @Test
    fun findConfigData_returnsNull_whenDataIdMissing() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))

        val source = finder.findConfigData("not-registered.yaml")

        assertNull(source, "查找不存在的 dataId 应返回 null")
    }

    @Test
    fun findConfigData_nullName_returnsNull() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))
        // dataId 字段非空，传入 null 不应命中任何条目
        assertNull(finder.findConfigData(null))
    }

    @Test
    fun findConfigData_multipleSources_picksByDataId() {
        register(dataId = "service-a.yaml", group = "DEFAULT_GROUP", payload = mapOf("svc" to "A"))
        register(dataId = "service-b.yaml", group = "DEFAULT_GROUP", payload = mapOf("svc" to "B"))

        val a = finder.findConfigData("service-a.yaml")
        val b = finder.findConfigData("service-b.yaml")

        assertEquals("A", a?.getProperty("svc"))
        assertEquals("B", b?.getProperty("svc"))
    }

    @Test
    fun serviceLoader_resolvesNacosConfigDataFinder() {
        val loaded = ServiceLoader.load(IConfigDataFinder::class.java).toList()
        assertTrue(
            loaded.any { it::class == NacosConfigDataFinder::class },
            "META-INF/services/io.kudos.context.config.IConfigDataFinder 应当注册 NacosConfigDataFinder，实际加载到: ${loaded.map { it::class.qualifiedName }}",
        )
    }

    @Test
    fun findConfigData_appliesConfiguredDecryptorToStringValues() {
        register(dataId = "secret.yaml", group = "DEFAULT_GROUP", payload = mapOf("password" to "ENC(cipher)"))
        val finder = NacosConfigDataFinder(listOf(TestDecryptor()))

        val source = finder.findConfigData("secret.yaml")

        assertEquals("plain:cipher", source?.getProperty("password"))
    }

    private class TestDecryptor : NacosConfigValueDecryptor {
        override fun supports(value: String): Boolean = value.startsWith("ENC(") && value.endsWith(")")

        override fun decrypt(value: String): String = "plain:" + value.removePrefix("ENC(").removeSuffix(")")
    }

    /**
     * 用反射清空 [NacosPropertySourceRepository] 的静态 ConcurrentHashMap。
     *
     * 公开 API 没提供 clear——nacos 在生产中假设 PropertySource 只增不减、与 spring 配置上
     * 下文生命周期对齐。测试场景下必须 between-test 清理，否则用例顺序影响断言。
     */
    private fun clearRepoViaReflection() {
        val field = NacosPropertySourceRepository::class.java
            .getDeclaredField("NACOS_PROPERTY_SOURCE_REPOSITORY")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = field.get(null) as ConcurrentHashMap<String, NacosPropertySource>
        map.clear()
    }

    /** 构造一份 [NacosPropertySource] 并塞进 repository。 */
    private fun register(dataId: String, group: String, payload: Map<String, Any>) {
        val inner: PropertySource<*> = MapPropertySource(dataId, payload)
        val src = NacosPropertySource(listOf(inner), group, dataId, Date(), /* refreshable = */ false)
        NacosPropertySourceRepository.collectNacosPropertySource(src)
    }
}
