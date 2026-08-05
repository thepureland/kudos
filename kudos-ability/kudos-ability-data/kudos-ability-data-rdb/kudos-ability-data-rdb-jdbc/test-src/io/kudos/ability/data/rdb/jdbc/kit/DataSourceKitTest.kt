package io.kudos.ability.data.rdb.jdbc.kit

import com.zaxxer.hikari.HikariDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DataSourceKit.createDataSource]: driver / test-query inference from the url,
 * optional catalog / schema handling, and null password support.
 *
 * (The getCurrentDataSource() forwarding is asserted in [RdbKitUnitTest] together with the
 * context-data-source branch.)
 *
 * @author K
 * @since 1.0.0
 */
internal class DataSourceKitTest {

    @Test
    fun createDataSource_infersDriverAndTestQueryFromUrl() {
        val url = "jdbc:h2:mem:dskit1;DB_CLOSE_DELAY=-1"
        val ds = DataSourceKit.createDataSource(url, "sa", null) as HikariDataSource
        ds.use {
            assertEquals(url, it.jdbcUrl)
            assertEquals("sa", it.username)
            assertNull(it.password)
            assertEquals("org.h2.Driver", it.driverClassName)
            assertEquals("select 1", it.connectionTestQuery)
            assertNull(it.catalog, "catalog not given -> left unset")
            assertNull(it.schema, "schema not given -> left unset")
        }
    }

    @Test
    fun createDataSource_setsCatalogAndSchemaWhenGiven() {
        val ds = DataSourceKit.createDataSource(
            "jdbc:h2:mem:dskit2;DB_CLOSE_DELAY=-1", "sa", "pwd",
            catalog = "cat1", schema = "PUBLIC"
        ) as HikariDataSource
        ds.use {
            assertEquals("cat1", it.catalog)
            assertEquals("PUBLIC", it.schema)
            assertEquals("pwd", it.password)
        }
    }

    @Test
    fun createDataSource_postgresUrlGetsPostgresDriver() {
        val ds = DataSourceKit.createDataSource("jdbc:postgresql://localhost:5432/db", "u", "p") as HikariDataSource
        ds.use {
            assertEquals("org.postgresql.Driver", it.driverClassName)
            assertEquals("select 1", it.connectionTestQuery)
        }
    }

    @Test
    fun createDataSource_producesWorkingH2Pool() {
        val ds = DataSourceKit.createDataSource("jdbc:h2:mem:dskit3;DB_CLOSE_DELAY=-1", "sa", "")
        (ds as HikariDataSource).use {
            it.connection.use { conn ->
                conn.createStatement().use { st ->
                    val rs = st.executeQuery("select 1")
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt(1))
                }
            }
        }
    }
}
