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

    /**
     * Filters [autoIncludes] down to the includes that can actually be resolved by [conf]'s template
     * loader. An include that fails to load (e.g. a template scheme without `macro.include`) is
     * silently skipped instead of failing the whole template-reading flow.
     *
     * Note: this probes by mutating [conf]'s auto-include list; callers are expected to call
     * `conf.setAutoIncludes(result)` afterwards (as [TemplateReader] does).
     *
     * @param conf Freemarker configuration whose template loader is used for probing
     * @param autoIncludes candidate include paths
     * @return the subset of [autoIncludes] that resolved successfully
     * @author K
     * @since 1.0.0
     */
    fun getAvailableAutoInclude(conf: Configuration, autoIncludes: List<String>): List<String?> =
        autoIncludes.filter { include ->
            runCatching {
                val template = Template("__auto_include_test__", StringReader("1"), conf)
                conf.setAutoIncludes(listOf(include))
                template.process(emptyMap<Any?, Any?>(), StringWriter())
            }.isSuccess
        }

    /**
     * Renders [template] with [model] and writes the result to [outputFile] using [encoding].
     *
     * @author K
     * @since 1.0.0
     */
    fun processTemplate(template: Template, model: Map<String, *>, outputFile: File, encoding: String) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(outputFile), encoding)).use {
            template.process(model, it)
        }
    }

    /**
     * Renders an inline template string with [model] and returns the result.
     * Used mainly to resolve placeholders (e.g. the entity-name placeholder) inside template file paths.
     *
     * @throws IllegalStateException when the template string cannot be parsed or rendered;
     *         the original Freemarker exception is preserved as the cause
     * @author K
     * @since 1.0.0
     */
    fun processTemplateString(templateString: String, model: Map<String, *>, conf: Configuration): String =
        StringWriter().use { writer ->
            try {
                Template("templateString...", StringReader(templateString), conf).process(model, writer)
                writer.toString()
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse template string: $templateString", e)
            }
        }

}