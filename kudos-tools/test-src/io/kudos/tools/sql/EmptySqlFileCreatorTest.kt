package io.kudos.tools.sql

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.*

/**
 * test for EmptySqlFileCreator
 *
 * Covers:
 * - buildEmptySqlFileName: version-prefix preservation, deterministic timestamp formatting and the
 *   fixed suffix; Unicode is irrelevant since the name is derived purely from the 5-char prefix.
 * - createEmptySqlFile: picks the latest (descending name) .sql file, writes an empty file named via
 *   buildEmptySqlFileName, ignores non-sql files, and fails (NoSuchElementException) when the
 *   directory has no .sql file.
 * - the placeholder class is still instantiable.
 *
 * The hard-coded-path main() entry itself is not unit-testable and is recorded as uncovered.
 *
 * @author K
 * @since 1.0.0
 */
internal class EmptySqlFileCreatorTest {

    @Test
    fun placeholderClassIsInstantiable() {
        assertNotNull(EmptySqlFileCreator())
    }

    @Test
    fun buildEmptySqlFileNameKeepsVersionPrefixAndFormatsTimestamp() {
        // 2021-02-03 04:05:06 in the default JVM time zone
        val cal = java.util.Calendar.getInstance().apply {
            clear(); set(2021, java.util.Calendar.FEBRUARY, 3, 4, 5, 6)
        }
        val now = cal.time
        val expectedTime = SimpleDateFormat("yyMMddHHmmss").format(now)

        val name = buildEmptySqlFileName("V1.0.0.9__init_xxx.sql", now)

        // version prefix = first 5 chars "V1.0.", joined to the timestamp with a literal '.'
        assertEquals("V1.0..${expectedTime}__x_xxxx.sql", name)
        assertTrue(name.startsWith("V1.0."), "the leading 5-char version prefix must be preserved")
        assertTrue(name.endsWith("__x_xxxx.sql"), "fixed suffix must be appended")
    }

    @Test
    fun createEmptySqlFilePicksLatestSqlAndWritesEmptyFile() {
        val dir = Files.createTempDirectory("empty-sql").toFile()
        try {
            File(dir, "V1.0.0.1__a.sql").writeText("-- old")
            File(dir, "V1.0.0.3__c.sql").writeText("-- newest by descending name")
            File(dir, "V1.0.0.2__b.sql").writeText("-- middle")
            File(dir, "readme.txt").writeText("not a sql file")

            val now = Date()
            val expectedName = buildEmptySqlFileName("V1.0.0.3__c.sql", now)
            val created = createEmptySqlFile(dir.absolutePath + File.separator, now)

            assertEquals(expectedName, created.name, "name must derive from the latest (descending) sql file")
            assertTrue(created.exists())
            assertEquals("", created.readText(), "the generated file must be empty")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun createEmptySqlFileFailsWhenNoSqlFilePresent() {
        val dir = Files.createTempDirectory("empty-sql-none").toFile()
        try {
            File(dir, "notes.txt").writeText("no sql here")
            assertFailsWith<NoSuchElementException> {
                createEmptySqlFile(dir.absolutePath + File.separator, Date())
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
