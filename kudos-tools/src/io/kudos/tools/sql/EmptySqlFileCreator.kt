package io.kudos.tools.sql

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Placeholder class: exists only so this file can be packaged into a jar (Kotlin requires
 * at least one top-level declaration). The actual logic lives in the [main] function below.
 *
 * @author K
 * @since 1.0.0
 */
class EmptySqlFileCreator

/**
 * Generate an empty SQL file with a standard filename in the current directory. Distributed as a jar in each resources/sql directory.
 *
 * @author K
 * @since 1.0.0
 */
fun main() {    //TODO how to package this class alone as a jar into the resources/sql directory
    createEmptySqlFile("D:/dev/kudos/kudos-data/kudos-data-jdbc/resources/sql/")
}

/**
 * Scans [path] (depth 1) for the latest `.sql` file by descending name order, derives the new file
 * name via [buildEmptySqlFileName], and writes an empty file with that name into [path].
 *
 * Extracted from [main] so the pure naming logic is independently testable without the hard-coded
 * production path; behavior is unchanged.
 *
 * @param path directory to scan and write into (must end with a path separator)
 * @param now timestamp used for the filename, defaults to "right now"
 * @return the created empty file
 * @author K
 * @since 1.0.0
 */
internal fun createEmptySqlFile(path: String, now: Date = Date()): File {
    val file = File(path).walk()
        .maxDepth(1)
        .filter { it.extension == "sql" }
        .sortedDescending()
        .first()
    val fileName = buildEmptySqlFileName(file.name, now)
    return File(path + fileName).apply { writeText("") }
}

/**
 * Builds the empty-SQL file name from the latest existing file name: keeps the leading 5-char version
 * prefix, appends a `yyMMddHHmmss` timestamp and the fixed `__x_xxxx.sql` suffix.
 *
 * @param latestFileName the latest existing sql file name (its first 5 chars are the version prefix)
 * @param now timestamp formatted into the name
 * @return the new empty-sql file name
 * @author K
 * @since 1.0.0
 */
internal fun buildEmptySqlFileName(latestFileName: String, now: Date): String {
    val version = latestFileName.substring(0, 5)
    val time = SimpleDateFormat("yyMMddHHmmss").format(now)
    return "${version}.${time}__x_xxxx.sql"
}

