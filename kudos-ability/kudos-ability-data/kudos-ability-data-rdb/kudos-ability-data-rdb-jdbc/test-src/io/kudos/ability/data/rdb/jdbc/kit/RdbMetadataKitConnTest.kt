package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.TableTypeEnum
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-JDBC unit tests for [RdbMetadataKit] using an explicitly passed H2 in-memory connection
 * (covers the `conn != null` branch of withConn — the Spring-context branch is covered by
 * [RdbMetadataKitTest]).
 *
 * Exercises: BASE TABLE -> TABLE type mapping, views, primary / foreign keys, indexes, unique
 * constraints, the `__CODE` dictionary-column convention and nullability flags.
 *
 * @author K
 * @since 1.0.0
 */
internal class RdbMetadataKitConnTest {

    private lateinit var conn: Connection

    @BeforeTest
    fun setUp() {
        conn = DriverManager.getConnection("jdbc:h2:mem:metadata_conn_test;DB_CLOSE_DELAY=-1", "sa", "")
        conn.createStatement().use { st ->
            st.execute("DROP ALL OBJECTS")
            st.execute(
                """
                CREATE TABLE parent_table (
                    id INT NOT NULL,
                    CONSTRAINT pk_parent PRIMARY KEY (id)
                )
                """.trimIndent()
            )
            st.execute(
                """
                CREATE TABLE child_table (
                    id INT NOT NULL,
                    parent_id INT,
                    gender__code VARCHAR(8),
                    email VARCHAR(64),
                    note VARCHAR(255),
                    CONSTRAINT pk_child PRIMARY KEY (id),
                    CONSTRAINT fk_child_parent FOREIGN KEY (parent_id) REFERENCES parent_table (id),
                    CONSTRAINT uq_child_email UNIQUE (email)
                )
                """.trimIndent()
            )
            st.execute("CREATE INDEX idx_child_note ON child_table (note)")
            st.execute("CREATE VIEW child_view AS SELECT id FROM child_table")
            st.execute("COMMENT ON TABLE child_table IS 'child table comment'")
            st.execute("COMMENT ON COLUMN child_table.note IS '备注'")
        }
    }

    @AfterTest
    fun tearDown() {
        conn.createStatement().use { it.execute("DROP ALL OBJECTS") }
        conn.close()
    }

    @Test
    fun getTablesByType_returnsTablesOnly() {
        val tables = RdbMetadataKit.getTablesByType(TableTypeEnum.TABLE, conn = conn)
        val names = tables.map { it.name?.uppercase() }
        assertTrue("PARENT_TABLE" in names)
        assertTrue("CHILD_TABLE" in names)
        assertFalse("CHILD_VIEW" in names, "views must not be returned for type TABLE")
        assertTrue(tables.all { it.type == TableTypeEnum.TABLE }, "H2's 'BASE TABLE' raw type maps back to TABLE")
    }

    @Test
    fun getTablesByType_viewType() {
        val views = RdbMetadataKit.getTablesByType(TableTypeEnum.VIEW, conn = conn)
        val view = views.firstOrNull { it.name.equals("child_view", ignoreCase = true) }
        assertNotNull(view, "the created view must be discoverable")
        assertEquals(TableTypeEnum.VIEW, view.type)
    }

    @Test
    fun getTableByName_existingAndMissing() {
        val table = RdbMetadataKit.getTableByName("CHILD_TABLE", conn = conn)
        assertNotNull(table)
        assertEquals("CHILD_TABLE", table.name)
        assertEquals(TableTypeEnum.TABLE, table.type)
        assertEquals("child table comment", table.comment)
        assertTrue(table.toString().contains("CHILD_TABLE"), "toString must include the table name")

        assertNull(RdbMetadataKit.getTableByName("NO_SUCH_TABLE", conn = conn))
    }

    @Test
    fun getColumnsByTableName_flagsKeysIndexesUniqueAndDictCode() {
        val columns = RdbMetadataKit.getColumnsByTableName("CHILD_TABLE", conn = conn)
        assertEquals(5, columns.size)

        val id = columns.getValue("ID")
        assertTrue(id.primaryKey)
        assertFalse(id.nullable)
        assertTrue(id.indexed, "PK columns are backed by an index")

        val parentId = columns.getValue("PARENT_ID")
        assertTrue(parentId.foreignKey)
        assertTrue(parentId.nullable)
        assertFalse(parentId.primaryKey)

        val dict = columns.getValue("GENDER__CODE")
        assertTrue(dict.dictCode, "columns ending with __CODE follow the kudos dictionary convention")
        assertFalse(columns.getValue("EMAIL").dictCode)

        val email = columns.getValue("EMAIL")
        assertTrue(email.unique)
        assertFalse(columns.getValue("NOTE").unique)

        val note = columns.getValue("NOTE")
        assertTrue(note.indexed)
        assertEquals("备注", note.comment, "unicode comments must round-trip")

        // type inference sanity (H2 -> Kotlin)
        assertEquals(Int::class, id.kotlinType)
        assertEquals(String::class, email.kotlinType)
        assertEquals(255, note.length)
    }
}
