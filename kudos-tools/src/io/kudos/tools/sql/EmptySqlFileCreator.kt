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
    val path = "D:/dev/kudos/kudos-data/kudos-data-jdbc/resources/sql/"
    val files = File(path).walk()
    val file = files.maxDepth(1)
        .filter { it.extension == "sql" }
        .sortedDescending()
        .first()
    val version = file.name.substring(0,5)
    val time = SimpleDateFormat("yyMMddHHmmss").format(Date())
    val fileName = "${version}.${time}__x_xxxx.sql"
    File(path + fileName).writeText("")
}

