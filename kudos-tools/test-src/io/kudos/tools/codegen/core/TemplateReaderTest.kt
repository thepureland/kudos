package io.kudos.tools.codegen.core

import io.kudos.tools.codegen.CodegenTestSupport
import java.io.StringWriter
import kotlin.test.*

/**
 * test for TemplateReader
 *
 * Covers: reading a template relative to the configured template scheme (UTF-8 output encoding,
 * Unicode content, placeholder rendering, macro.include auto-include resolution) and the
 * missing-template error path.
 *
 * @author K
 * @since 1.0.0
 */
internal class TemplateReaderTest {

    @BeforeTest
    fun setUp() {
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(
            templateName = "test_reader",
            templateRootDir = CodegenTestSupport.classpathDirPath("/templates/test_reader"),
        )
    }

    @Test
    fun readLoadsTemplateAndForcesUtf8OutputEncoding() {
        val template = TemplateReader().read("sub/hello.txt")
        assertEquals("UTF-8", template.outputEncoding)

        val writer = StringWriter()
        template.process(mapOf("name" to "K"), writer)
        assertEquals("Hello K!你好", writer.toString())
    }

    @Test
    fun readFailsForMissingTemplate() {
        assertFailsWith<java.io.IOException> { TemplateReader().read("sub/missing.txt") }
    }
}
