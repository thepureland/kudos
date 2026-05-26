package io.kudos.tools.codegen.core

import freemarker.template.Configuration
import freemarker.template.Template
import java.io.*

/**
 * Freemarker utility.
 *
 * @author K
 * @since 1.0.0
 */
object FreemarkerKit {

    fun getAvailableAutoInclude(conf: Configuration, autoIncludes: List<String>): List<String?> =
        autoIncludes.map { include ->
            val t = Template("__auto_include_test__", StringReader("1"), conf)
            conf.setAutoIncludes(listOf(include))
            t.process(HashMap<Any?, Any?>(), StringWriter())
            include
        }

    fun processTemplate(template: Template, model: Map<String, *>, outputFile: File, encoding: String) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(outputFile), encoding)).use {
            template.process(model, it)
        }
    }

    fun processTemplateString(templateString: String, model: Map<String, *>, conf: Configuration): String =
        StringWriter().use { writer ->
            try {
                Template("templateString...", StringReader(templateString), conf).process(model, writer)
                writer.toString()
            } catch (_: Exception) {
                error("Failed to parse template string: $templateString")
            }
        }

}