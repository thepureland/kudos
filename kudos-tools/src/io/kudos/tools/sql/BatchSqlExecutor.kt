package io.kudos.tools.sql

import io.kudos.base.io.FileKit
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Batch-executes SQL statements from a file.
 * The SQL comes from a local file (not user input) and is treated as trusted content, so the
 * "unsafe SQL string" warning for addBatch(line) is suppressed.
 *
 * @author K
 * @since 1.0.0
 */
fun main() {
    val file = "C:\\Users\\hanfei\\Desktop\\area2019.sql" // File encoding must be UTF-8 without BOM; otherwise strange errors occur.
    val lineIterator = FileKit.lineIterator(File(file), "UTF-8")

//    Class.forName("org.h2.Driver")
    val start = System.currentTimeMillis()
    val i = DriverManager.getConnection(
        "jdbc:h2:tcp://localhost:9092/D:/dev/kudos/kudos-data/kudos-data-jdbc/h2/h2",
        "sa",
        null
    ).use { conn -> batchExecute(conn, lineIterator) }

    val end = System.currentTimeMillis()
    println("Inserted $i rows, total elapsed: ${end - start}ms")
}

/**
 * Adds every SQL line from [lines] to a single [java.sql.Statement] batch, committing every
 * [batchSize] rows and flushing the trailing partial batch at the end. [conn]'s auto-commit is
 * turned off so each [batchSize]-row chunk commits as one transaction.
 *
 * Extracted from [main] so the batching/commit logic is testable against any [Connection]
 * (e.g. in-memory H2) instead of the hard-coded TCP server; behavior is unchanged.
 *
 * @param conn the JDBC connection (auto-commit will be set to false)
 * @param lines iterator over SQL statements (one statement per element)
 * @param batchSize number of statements per commit, defaults to [BATCH_SIZE]
 * @return the total number of statements added to the batch
 * @author K
 * @since 1.0.0
 */
internal fun batchExecute(conn: Connection, lines: Iterator<String>, batchSize: Int = BATCH_SIZE): Int {
    var i = 0
    conn.autoCommit = false
    conn.createStatement().use { stm ->
        while (lines.hasNext()) {
            val line = lines.next()
            @Suppress("SqlSourceToSinkFlow")
            stm.addBatch(line)
            i++
            if (i % batchSize == 0) {
                stm.executeBatch()
                conn.commit()
                println("Committed $i rows in total")
            }
        }
        // Flush the remaining partial batch
        stm.executeBatch()
        conn.commit()
    }
    return i
}

/** Number of statements per JDBC batch commit */
internal const val BATCH_SIZE = 5000