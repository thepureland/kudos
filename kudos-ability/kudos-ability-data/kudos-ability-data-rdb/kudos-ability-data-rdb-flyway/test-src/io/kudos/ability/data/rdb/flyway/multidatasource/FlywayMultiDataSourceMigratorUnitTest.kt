package io.kudos.ability.data.rdb.flyway.multidatasource

import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit tests for [FlywayMultiDataSourceMigrator] (no Spring container; collaborators are
 * injected via reflection, [IFlywayDataSourceResolver] is mocked). Covers:
 * - empty execution plan: migrate() returns early and never touches the data source layer
 * - auto-config mode: an on-disk `sql/<module>/` without a datasource-config mapping aborts startup
 * - auto-config happy path: when every on-disk module is mapped, the auto check passes and the
 *   migrator proceeds to the data source layer
 * - configuration-source tracing: falls back to "unknown" when no property source declares
 *   datasource-config, and names the contributing property source when one does
 * - duplicate module detection: the same module name under two classpath URLs aborts startup
 * - jar-protocol classpath scanning: modules inside a jar's `sql/` directory are discovered
 * - a classpath entry where `sql` is a plain file (not a directory) contributes no modules
 * - `checkNotNull` branch: data source key exists in the routing table but resolves to null
 * - execution-order reordering: listed keys run first; blank entries and unknown keys are ignored
 *
 * Integration behavior (real migration, placeholder replacement, missing-ds error via Spring
 * context) is covered by [io.kudos.ability.data.rdb.flyway.FlywayTest].
 *
 * @author K
 * @since 1.0.0
 */
internal class FlywayMultiDataSourceMigratorUnitTest {

    private fun inject(target: Any, fieldName: String, value: Any) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun newMigrator(
        props: FlywayMultiDataSourceProperties,
        dataSourceResolver: IFlywayDataSourceResolver = Mockito.mock(IFlywayDataSourceResolver::class.java),
        environment: ConfigurableEnvironment = StandardEnvironment()
    ): FlywayMultiDataSourceMigrator {
        val migrator = FlywayMultiDataSourceMigrator()
        inject(migrator, "flywayMultiDatasourceProperties", props)
        inject(migrator, "dataSourceResolver", dataSourceResolver)
        inject(migrator, "environment", environment)
        return migrator
    }

    /**
     * Runs [block] with the thread context classloader temporarily extended by [extraUrls].
     * The loader is closed afterwards so jar files are released (Windows would otherwise keep
     * them locked and break @TempDir cleanup).
     */
    private fun withExtraClasspath(vararg extraUrls: java.net.URL, block: () -> Unit) {
        val thread = Thread.currentThread()
        val original = thread.contextClassLoader
        val loader = URLClassLoader(arrayOf(*extraUrls), original)
        thread.contextClassLoader = loader
        try {
            block()
        } finally {
            thread.contextClassLoader = original
            loader.close()
        }
    }

    /** Empty datasource-config → empty plan → early return, data source layer never consulted. */
    @Test
    fun migrateWithEmptyPlanIsNoOp() {
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        val migrator = newMigrator(FlywayMultiDataSourceProperties(), resolver)
        migrator.migrate()
        Mockito.verifyNoInteractions(resolver)
    }

    /**
     * auto-config.enabled=true requires every on-disk sql/<module>/ to be mapped; module_bad is
     * on the test classpath but unmapped here, so migrate() must abort with a message naming it.
     */
    @Test
    fun autoConfigAbortsOnUnmappedOnDiskModule() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to "module1")
        props.autoConfig.enabled = true
        val migrator = newMigrator(props)
        val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
        assertTrue(e.message!!.contains("module_bad"), "message should name the unmapped module: ${e.message}")
        assertTrue(e.message!!.contains("auto-config"), "message should mention auto-config: ${e.message}")
    }

    /**
     * auto-config.enabled=true with every on-disk module (module1 + module_bad) mapped: the auto
     * check must pass and execution must reach the data source layer. The mapped data source is
     * absent from the routing table, so the resulting error is the "data source does not exist"
     * one — proving the auto-config gate was cleared (it would otherwise name "auto-config").
     */
    @Test
    fun autoConfigWithAllModulesMappedProceedsToMigration() {
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        Mockito.`when`(resolver.hasDataSource("ds_all")).thenReturn(false)
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds_all" to "module1,module_bad")
        props.autoConfig.enabled = true
        val migrator = newMigrator(props, resolver)
        val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
        assertTrue(e.message!!.contains("[ds_all]"), "message should name the data source: ${e.message}")
        assertTrue(e.message!!.contains("does not exist"), "should be the missing-ds error: ${e.message}")
        assertFalse(e.message!!.contains("auto-config"), "auto-config gate should have been cleared: ${e.message}")
    }

    /**
     * Single-arg migrateByModule with a module absent from datasource-config and an environment
     * carrying no `kudos.ability.flyway.datasource-config.*` property at all: the source-tracing
     * hint must fall back to "unknown" instead of listing yml files.
     */
    @Test
    fun unknownModuleWithNoConfigSourcesFallsBackToUnknown() {
        val migrator = newMigrator(FlywayMultiDataSourceProperties())
        val e = assertFailsWith<IllegalStateException> { migrator.migrateByModule("ghost_module") }
        assertTrue(e.message!!.contains("ghost_module"), "message should name the module: ${e.message}")
        assertTrue(e.message!!.contains("no data source configured"), "unexpected cause: ${e.message}")
        assertTrue(e.message!!.contains("[unknown]"), "sources should fall back to unknown: ${e.message}")
    }

    /**
     * When a property source in the environment does declare a
     * `kudos.ability.flyway.datasource-config.*` entry, error messages must name it (falling back
     * to the raw property source name when YamlPropertySourceFactory has no yml mapping for it).
     */
    @Test
    fun autoConfigErrorNamesContributingPropertySource() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "unit-test-config-source",
                mapOf("kudos.ability.flyway.datasource-config.ds1" to "module1")
            )
        )
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to "module1")
        props.autoConfig.enabled = true
        val migrator = newMigrator(props, environment = environment)
        val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
        assertTrue(e.message!!.contains("module_bad"), "message should name the unmapped module: ${e.message}")
        assertTrue(
            e.message!!.contains("unit-test-config-source"),
            "message should name the contributing property source: ${e.message}"
        )
    }

    /** The same module name under two classpath sql/ roots cannot share one history table → abort. */
    @Test
    fun duplicateModuleNameOnClasspathAborts(@TempDir tempDir: File) {
        val duplicated = File(tempDir, "sql/module1")
        assertTrue(duplicated.mkdirs())
        withExtraClasspath(tempDir.toURI().toURL()) {
            val migrator = newMigrator(FlywayMultiDataSourceProperties())
            val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
            assertTrue(e.message!!.contains("module1"), "message should name the duplicate module: ${e.message}")
        }
    }

    /**
     * Modules packaged inside a jar (jar protocol URL) must be discovered too. The discovered
     * module is mapped to a data source missing from the routing table, so the error message
     * proves the jar scan actually found it (an unscanned module would be orphan-skipped silently).
     */
    @Test
    fun jarProtocolModulesAreScanned(@TempDir tempDir: File) {
        val jarFile = File(tempDir, "flyway-modules.jar")
        JarOutputStream(jarFile.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry("sql/")); jar.closeEntry()
            jar.putNextEntry(JarEntry("sql/modjar/")); jar.closeEntry()
            jar.putNextEntry(JarEntry("sql/modjar/h2/")); jar.closeEntry()
            jar.putNextEntry(JarEntry("sql/modjar/h2/V1.0.1__a.sql"))
            jar.write("create table t_jar(id int);".toByteArray())
            jar.closeEntry()
        }
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        Mockito.`when`(resolver.hasDataSource("ds_jar")).thenReturn(false)
        withExtraClasspath(jarFile.toURI().toURL()) {
            val props = FlywayMultiDataSourceProperties()
            props.datasourceConfig = linkedMapOf("ds_jar" to "modjar")
            val migrator = newMigrator(props, resolver)
            val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
            assertTrue(e.message!!.contains("ds_jar"), "message should name the data source: ${e.message}")
            assertTrue(e.message!!.contains("modjar"), "message should name the jar module: ${e.message}")
        }
    }

    /**
     * A classpath entry where `sql` is a plain file rather than a directory: listFiles() returns
     * null and the entry must contribute no modules. The configured module is then an orphan
     * (warn + skip), the plan ends up empty and the data source layer is never consulted.
     */
    @Test
    fun sqlRootAsPlainFileContributesNoModules(@TempDir tempDir: File) {
        val sqlAsFile = File(tempDir, "sql")
        assertTrue(sqlAsFile.createNewFile(), "should create a plain file named sql")
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        withExtraClasspath(tempDir.toURI().toURL()) {
            val props = FlywayMultiDataSourceProperties()
            props.datasourceConfig = linkedMapOf("ds_file" to "module_in_file_entry")
            val migrator = newMigrator(props, resolver)
            migrator.migrate() // must not throw: orphan module is warned and skipped
            Mockito.verifyNoInteractions(resolver)
        }
    }

    /** hasDataSource=true but getDataSource=null → checkNotNull must fail with a clear message. */
    @Test
    fun nullResolvedDataSourceAborts() {
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        Mockito.`when`(resolver.hasDataSource("ds_null")).thenReturn(true)
        Mockito.`when`(resolver.getDataSource("ds_null")).thenReturn(null)
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds_null" to "module1")
        val migrator = newMigrator(props, resolver)
        val e = assertFailsWith<IllegalStateException> { migrator.migrateByModule("module1", "ds_null") }
        assertTrue(e.message!!.contains("resolved to null"), "unexpected message: ${e.message}")
        assertTrue(e.message!!.contains("ds_null"), "message should name the data source: ${e.message}")
    }

    /**
     * `spring.flyway.*` keys are not used by this module (it never runs Boot's Flyway
     * auto-configuration): they must be reported as ignored, except `spring.flyway.enabled`,
     * which this module sets itself to keep Boot's auto-configuration inert.
     */
    @Test
    fun ignoredSpringFlywayKeysAreReported() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "legacy-flyway-config",
                mapOf(
                    "spring.flyway.enabled" to "false",
                    "spring.flyway.out-of-order" to "true",
                    "spring.flyway.placeholders.app_schema" to "public"
                )
            )
        )
        val migrator = newMigrator(FlywayMultiDataSourceProperties(), environment = environment)
        assertEquals(
            setOf("spring.flyway.out-of-order", "spring.flyway.placeholders.app_schema"),
            migrator.warnOnIgnoredSpringFlywayKeys()
        )
    }

    /**
     * mode=dry-run: the execution plan is previewed via Flyway `info()` — pending migrations are
     * logged but nothing is applied: neither business tables nor the module history table may
     * exist afterwards.
     */
    @Test
    fun dryRunModePreviewsWithoutApplying() {
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        val ds = JdbcDataSource()
        ds.setURL("jdbc:h2:mem:flyway_migrator_dry_run;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;")
        ds.user = "sa"
        ds.password = "sa"
        Mockito.`when`(resolver.hasDataSource("ds_dry")).thenReturn(true)
        Mockito.`when`(resolver.getDataSource("ds_dry")).thenReturn(ds)
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds_dry" to "module1")
        props.mode = "dry-run"
        val migrator = newMigrator(props, resolver)
        migrator.migrate()
        ds.connection.use { conn ->
            conn.createStatement().use { statement ->
                statement.executeQuery(
                    "select count(*) from information_schema.tables where lower(table_name) like 'test_table_flyway%' or lower(table_name) like 'flyway_history%'"
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1), "dry-run must not create any table")
                }
            }
        }
    }

    /**
     * execution-order: blank entries are skipped, keys absent from the plan are dropped, listed
     * keys run before unlisted ones. With order [" ", "ds_b", "no_such_ds"], data source ds_b
     * (hosting module_bad) must be attempted before ds_a — proven by the failure naming ds_b.
     */
    @Test
    fun executionOrderReordersDataSources() {
        val resolver = Mockito.mock(IFlywayDataSourceResolver::class.java)
        Mockito.`when`(resolver.hasDataSource(Mockito.anyString())).thenReturn(false)
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds_a" to "module1", "ds_b" to "module_bad")
        props.executionOrder = listOf(" ", "ds_b", "no_such_ds")
        val migrator = newMigrator(props, resolver)
        val e = assertFailsWith<IllegalStateException> { migrator.migrate() }
        assertTrue(e.message!!.contains("[ds_b]"), "ds_b should be attempted first: ${e.message}")
        assertTrue(e.message!!.contains("[module_bad]"), "module_bad should be attempted first: ${e.message}")
    }
}
