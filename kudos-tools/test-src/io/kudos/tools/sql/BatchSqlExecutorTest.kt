package io.kudos.tools.sql

import org.h2.jdbcx.JdbcDataSource
import java.sql.Connection
import kotlin.test.*

/**
 * test for BatchSqlExecutor
 *
 * Covers the [batchExecute] batching/commit logic extracted from main():
 * - a small batchSize so several mid-stream commits plus the trailing partial flush are exercised;
 * - the exact-multiple-of-batchSize case (no trailing partial batch);
 * - empty input (only the trailing flush runs, returns 0);
 * - auto-commit is turned off on the supplied connection;
 * - the returned count equals the number of statements added.
 *
 * The hard-coded-path / TCP-server main() entry itself is not unit-testable and is recorded as
 * uncovered.
 *
 * @author K
 * @since 1.0.0
 */
internal class BatchSqlExecutorTest {

    private fun newH2Connection(dbName: String): Connection {
        val ds = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1")
            user = "sa"
        }
        val conn = ds.connection
        conn.createStatement().use { st ->
            st.execute("""DROP TABLE IF EXISTS "batch_demo"""")
            st.execute("""CREATE TABLE "batch_demo" ("v" INT)""")
        }
        return conn
    }

    private fun rowCount(conn: Connection): Int =
        conn.createStatement().use { st ->
            st.executeQuery("""SELECT COUNT(*) FROM "batch_demo"""").use { rs ->
                rs.next(); rs.getInt(1)
            }
        }

    @Test
    fun batchExecuteCommitsMidStreamAndFlushesTrailingPartialBatch() {
        newH2Connection("batch_partial").use { conn ->
            // 5 inserts with batchSize=2 -> commits at 2 and 4, trailing partial batch of 1 flushed
            val lines = (1..5).map { """INSERT INTO "batch_demo" ("v") VALUES ($it)""" }
            val total = batchExecute(conn, lines.iterator(), batchSize = 2)

            assertEquals(5, total, "all statements must be counted")
            assertFalse(conn.autoCommit, "auto-commit must be turned off")
            assertEquals(5, rowCount(conn), "every row including the trailing partial batch must be persisted")
        }
    }

    @Test
    fun batchExecuteHandlesExactMultipleOfBatchSize() {
        newH2Connection("batch_exact").use { conn ->
            val lines = (1..4).map { """INSERT INTO "batch_demo" ("v") VALUES ($it)""" }
            val total = batchExecute(conn, lines.iterator(), batchSize = 2)

            assertEquals(4, total)
            assertEquals(4, rowCount(conn), "exact-multiple case must persist all rows via the trailing empty flush")
        }
    }

    @Test
    fun batchExecuteWithNoLinesCommitsNothingAndReturnsZero() {
        newH2Connection("batch_empty").use { conn ->
            val total = batchExecute(conn, emptyList<String>().iterator(), batchSize = 2)

            assertEquals(0, total)
            assertEquals(0, rowCount(conn))
            assertFalse(conn.autoCommit)
        }
    }

    @Test
    fun batchSizeConstantIsFiveThousand() {
        assertEquals(5000, BATCH_SIZE)
    }
}
