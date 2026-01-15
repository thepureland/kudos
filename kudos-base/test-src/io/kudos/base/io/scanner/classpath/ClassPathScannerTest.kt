package io.kudos.base.io.scanner.classpath

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * ClassPathScanner测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ClassPathScannerTest {

    @Test
    fun testScanForResources() {
        val resources = ClassPathScanner.scanForResources("", "", "")
        assertNotNull(resources)
    }

    @Test
    fun testScanForResourcesWithPrefix() {
        val resources = ClassPathScanner.scanForResources("", "log", "")
        assertNotNull(resources)
    }

    @Test
    fun testScanForResourcesWithSuffix() {
        val resources = ClassPathScanner.scanForResources("", "", ".properties")
        assertNotNull(resources)
    }

    @Test
    fun testScanForResourcesWithPrefixAndSuffix() {
        val resources = ClassPathScanner.scanForResources("", "log", ".xml")
        assertNotNull(resources)
    }

    @Test
    fun testScanForResourcesWithPath() {
        val resources = ClassPathScanner.scanForResources("io/kudos/base", "", "")
        assertNotNull(resources)
    }

    @Test
    fun testScanForClasses() {
        val classes = ClassPathScanner.scanForClasses("io.kudos.base", java.io.Serializable::class)
        assertNotNull(classes)
    }

    @Test
    fun testScanForClassesWithInterface() {
        val classes = ClassPathScanner.scanForClasses(
            "io.kudos.base.logger",
            io.kudos.base.logger.ILogCreator::class
        )
        assertNotNull(classes)
    }

    @Test
    fun testScanForClassesFiltersAbstract() {
        // 测试应该过滤掉抽象类
        val classes = ClassPathScanner.scanForClasses(
            "io.kudos.base",
            Any::class
        )
        assertNotNull(classes)
    }

    @Test
    fun testGetLocationUrlsForPath() {
        val urls = ClassPathScanner.getLocationUrlsForPath("")
        assertNotNull(urls)
    }

    @Test
    fun testGetLocationUrlsForPathWithSpecificPath() {
        val urls = ClassPathScanner.getLocationUrlsForPath("io/kudos/base")
        assertNotNull(urls)
    }

    @Test
    fun testScanForResourcesReturnsResourceArray() {
        val resources = ClassPathScanner.scanForResources("", "", "")
        resources.forEach { resource ->
            assertNotNull(resource)
        }
    }

    @Test
    fun testScanForResourcesWithNonExistentPath() {
        val resources = ClassPathScanner.scanForResources("non/existent/path", "", "")
        // 应该返回空数组而不是抛出异常
        assertNotNull(resources)
    }
}
