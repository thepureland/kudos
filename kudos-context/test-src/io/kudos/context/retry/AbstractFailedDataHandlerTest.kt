package io.kudos.context.retry

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for AbstractFailedDataHandler
 *
 * Coverage:
 * - persistFailedData: directory auto-creation, "{timestamp}-{uuid}.json" naming, JSON content,
 *   absolute path return value, and reuse of an existing directory on the second write
 * - handleFailedData: reads the file back, deserializes via the reflected generic type, and
 *   delegates to processFailedData (both true and false outcomes)
 * - readDataFromFile IOException path: a missing file is wrapped in RuntimeException("Read failed data error")
 * - persistFailedData IOException path: an unwritable location is wrapped in
 *   RuntimeException("Persist failed data error")
 * - Unicode round-trip through the JSON file
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractFailedDataHandlerTest {

    private lateinit var tempRoot: Path

    @BeforeTest
    fun setup() {
        tempRoot = createTempDirectory("kudos-afdh-test-")
    }

    @AfterTest
    fun cleanup() {
        tempRoot.toFile().walkBottomUp().forEach { it.delete() }
    }

    /** Concrete handler over String (built-in serializer, no @Serializable needed). */
    private inner class StringHandler(
        private val processFn: (String) -> Boolean
    ) : AbstractFailedDataHandler<String>() {
        override val businessType: String = "string-biz"
        override val cronExpression: String = "0 0/5 * * * *"
        val processedData = mutableListOf<String>()
        override fun filePath(): String = tempRoot.toString()
        override fun processFailedData(data: String): Boolean {
            processedData += data
            return processFn(data)
        }
    }

    @Test
    fun persistCreatesDirectoryAndWritesTimestampUuidJsonFile() {
        val handler = StringHandler { true }
        val path = handler.persistFailedData("payload-1")

        val file = File(path)
        assertTrue(file.isAbsolute, "the returned path must be absolute")
        assertTrue(file.exists(), "the persisted file must exist")
        assertEquals(tempRoot.resolve("string-biz").toFile(), file.parentFile, "data goes under {filePath}/{businessType}")
        assertTrue(
            Regex("""\d+-[0-9a-fA-F\-]+\.json""").matches(file.name),
            "file name must follow {timestamp}-{uuid}.json: ${file.name}"
        )
        assertEquals("\"payload-1\"", file.readText(), "the content must be the JSON serialization of the data")
    }

    @Test
    fun persistReusesExistingDirectoryAndKeepsFilesUnique() {
        val handler = StringHandler { true }
        val p1 = handler.persistFailedData("a")
        val p2 = handler.persistFailedData("b")
        assertTrue(p1 != p2, "every persisted file must get a unique name")
        assertTrue(File(p1).exists() && File(p2).exists())
    }

    @Test
    fun handleFailedDataReadsBackAndDelegatesTrueOutcome() {
        val handler = StringHandler { true }
        val path = handler.persistFailedData("漢字-ünïcode-✓")

        val result = handler.handleFailedData(File(path))

        assertTrue(result)
        assertEquals(listOf("漢字-ünïcode-✓"), handler.processedData, "Unicode must survive the JSON round-trip")
    }

    @Test
    fun handleFailedDataPropagatesFalseOutcome() {
        val handler = StringHandler { false }
        val path = handler.persistFailedData("will-fail")
        assertFalse(handler.handleFailedData(File(path)))
        assertEquals(listOf("will-fail"), handler.processedData)
    }

    @Test
    fun readingMissingFileWrapsIoExceptionInRuntimeException() {
        val handler = StringHandler { true }
        val missing = tempRoot.resolve("string-biz").resolve("0-no-such.json").toFile()
        val e = assertFailsWith<RuntimeException> { handler.handleFailedData(missing) }
        assertEquals("Read failed data error", e.message)
        assertTrue(e.cause is java.io.IOException, "the original IOException must be preserved as the cause")
    }

    @Test
    fun persistIntoUnwritableLocationWrapsIoExceptionInRuntimeException() {
        // Make {filePath()}/{businessType} unwritable by occupying {filePath()} with a regular FILE
        val blocking = tempRoot.resolve("blocked-as-file")
        Files.write(blocking, byteArrayOf(1))
        val handler = object : AbstractFailedDataHandler<String>() {
            override val businessType: String = "sub"
            override val cronExpression: String = "0 0/5 * * * *"
            override fun filePath(): String = blocking.toString()
            override fun processFailedData(data: String): Boolean = true
        }
        val e = assertFailsWith<RuntimeException> { handler.persistFailedData("x") }
        assertEquals("Persist failed data error", e.message)
        assertTrue(e.cause is java.io.IOException)
    }
}
