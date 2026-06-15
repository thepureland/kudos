package io.kudos.tools.codegen.core

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import kotlin.test.*

/**
 * test for FreemarkerKit
 *
 * @author K
 * @since 1.0.0
 */
internal class FreemarkerKitTest {

    @Test
    fun processTemplateStringResolvesPlaceholders() {
        val result = FreemarkerKit.processTemplateString(
            "\${name}.kt", mapOf("name" to "User"), newConfiguration()
        )
        assertEquals("User.kt", result)
    }

    @Test
    fun processTemplateStringFailurePreservesCause() {
        val e = assertFailsWith<IllegalStateException> {
            FreemarkerKit.processTemplateString("<#if>", emptyMap<String, Any>(), newConfiguration())
        }
        assertNotNull(e.cause, "the original Freemarker exception must be kept as the cause")
    }

    @Test
    fun getAvailableAutoIncludeSkipsUnresolvableIncludes() {
        val conf = newConfiguration().apply {
            templateLoader = StringTemplateLoader().apply { putTemplate("macro.include", "") }
        }
        val available = FreemarkerKit.getAvailableAutoInclude(conf, listOf("macro.include", "missing.include"))
        assertEquals(listOf("macro.include"), available)
    }

    @Test
    fun getAvailableAutoIncludeReturnsEmptyWhenNothingResolves() {
        val conf = newConfiguration().apply { templateLoader = StringTemplateLoader() }
        assertTrue(FreemarkerKit.getAvailableAutoInclude(conf, listOf("missing.include")).isEmpty())
    }

    @Test
    fun processTemplateWritesRenderedContentToFileWithEncoding() {
        val conf = newConfiguration().apply {
            templateLoader = StringTemplateLoader().apply { putTemplate("t", "你好, \${name}!") }
        }
        val template = conf.getTemplate("t")
        val outputFile = java.io.File.createTempFile("freemarker-kit", ".txt").apply { deleteOnExit() }

        FreemarkerKit.processTemplate(template, mapOf("name" to "K"), outputFile, "UTF-8")

        assertEquals("你好, K!", outputFile.readText(Charsets.UTF_8))
    }

    private fun newConfiguration() = Configuration(Configuration.VERSION_2_3_30)
}
