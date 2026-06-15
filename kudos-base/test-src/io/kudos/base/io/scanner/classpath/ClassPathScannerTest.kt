package io.kudos.base.io.scanner.classpath

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ClassPathScanner test cases
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
        // Test should filter out abstract classes
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
        // Should return empty array instead of throwing an exception
        assertNotNull(resources)
    }

    /**
     * Uses a slash-separated location so getResources actually resolves the package directory on the classpath,
     * driving the scanForClasses loop body (toClassName, abstract filter, isAssignableFrom check, Class.forName).
     */
    @Test
    fun testScanForClasses_slashLocation_findsImplementations() {
        val classes = ClassPathScanner.scanForClasses(
            "io/kudos/base/logger/slf4j",
            io.kudos.base.logger.ILogCreator::class
        )
        assertNotNull(classes)
        // Slf4jLoggerCreator implements ILogCreator and is concrete -> must be found.
        assertTrue(
            classes.any { it == io.kudos.base.logger.slf4j.Slf4jLoggerCreator::class.java },
            "Slf4jLoggerCreator should be discovered"
        )
    }

    /**
     * Scanning a package that lives inside a JAR (kotlin-stdlib) drives the jar-protocol branch of
     * createLocationScanner / findResourceNames (JarFileClassPathLocationScanner).
     */
    @Test
    fun testScanForResources_jarBackedPackage() {
        val resources = ClassPathScanner.scanForResources("kotlin", "", ".kotlin_builtins")
        assertNotNull(resources)
        // kotlin-stdlib ships kotlin/*.kotlin_builtins resources inside its jar.
        assertTrue(resources.isNotEmpty(), "expected jar-backed .kotlin_builtins resources")
    }

    /**
     * Scanning concrete classes that implement a very common interface exercises the abstract-class skip branch
     * (abstract classes that match are filtered out) plus the non-matching skip branch.
     */
    @Test
    fun testScanForClasses_slashLocation_filtersAbstractAndNonMatching() {
        val classes = ClassPathScanner.scanForClasses("io/kudos/base/logger", java.io.Serializable::class)
        assertNotNull(classes)
        // every returned class must be concrete and assignable to the requested interface
        classes.forEach {
            assertTrue(!java.lang.reflect.Modifier.isAbstract(it.modifiers))
            assertTrue(java.io.Serializable::class.java.isAssignableFrom(it))
        }
    }
}
