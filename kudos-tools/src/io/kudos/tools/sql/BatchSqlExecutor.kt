package io.kudos.tools.sql

import io.kudos.base.io.FileKit
import java.io.File
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
    var i = 0
    DriverManager.getConnection(
        "jdbc:h2:tcp://localhost:9092/D:/dev/kudos/kudos-data/kudos-data-jdbc/h2/h2",
        "sa",
        null
    ).use { conn ->
        conn.autoCommit = false
        conn.createStatement().use { stm ->
            while (lineIterator.hasNext()) {
                val line = lineIterator.next()
                @Suppress("SqlSourceToSinkFlow")
                stm.addBatch(line)
                i++
                if (i % BATCH_SIZE == 0) {
                    stm.executeBatch()
                    conn.commit()
                    println("Committed $i rows in total")
                }
            }
            // Flush the remaining partial batch
            stm.executeBatch()
            conn.commit()
        }
    }

    val end = System.currentTimeMillis()
    println("Inserted $i rows, total elapsed: ${end - start}ms")
}

/** Number of statements per JDBC batch commit */
private const val BATCH_SIZE = 5000