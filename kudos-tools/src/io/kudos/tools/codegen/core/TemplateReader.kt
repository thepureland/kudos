package io.kudos.tools.codegen.core

import freemarker.cache.MultiTemplateLoader
import freemarker.cache.URLTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import io.kudos.base.logger.LogFactory
import java.net.URI
import java.net.URL


/**
 * Template content reader.
 *
 * @author K
 * @since 1.0.0
 */
class TemplateReader {

    /**
     * Reads the specified template under the template root directory and returns a Freemarker [Template];
     * output encoding is forced to UTF-8.
     *
     * @param templateFileRelativePath template path relative to the template root directory
     * @return the loaded Freemarker [Template]
     * @author K
     * @since 1.0.0
     */
    fun read(templateFileRelativePath: String): Template =
        newFreeMarkerConfiguration().getTemplate(templateFileRelativePath).apply {
            outputEncoding = "UTF-8"
        }

    /**
     * Builds a Freemarker [Configuration] supporting both "filesystem templates" and "classpath templates":
     * - [URLTemplateLoader]: lets users edit templates in an external directory without rebuilding.
     * - `setClassForTemplateLoading`: fallback for built-in templates packaged inside the jar.
     *
     * Also registers `macro.include` (if present) as an auto-include so templates do not need a manual
     * `<#include>` for shared macros.
     *
     * @return an initialized Freemarker configuration
     * @author K
     * @since 1.0.0
     */
    private fun newFreeMarkerConfiguration(): Configuration {
        val templateRootDir = CodeGeneratorContext.config.getTemplateInfo().rootDir
        val root = URI("file:$templateRootDir").toURL()
        val loader = MultiTemplateLoader(arrayOf(
            object : URLTemplateLoader() {
                override fun getURL(template: String): URL = root.toURI().resolve(template).toURL()
            }
        ))
        return Configuration(Configuration.VERSION_2_3_30).apply {
            templateLoader = loader
            numberFormat = "###############"
            booleanFormat = "true,false"
            defaultEncoding = "UTF-8"
            setClassForTemplateLoading(
                CodeGenerator::class.java,
                "/templates/${CodeGeneratorContext.config.getTemplateInfo().name}"
            )
            val autoIncludes = listOf("macro.include")
            val available = FreemarkerKit.getAvailableAutoInclude(this, autoIncludes)
            setAutoIncludes(available)
            log.debug("set Freemarker.autoIncludes:$available for templateName:$templateRootDir autoIncludes:$autoIncludes")
        }
    }

    /** Logger; used only to print the auto-include resolution result */
    private val log = LogFactory.getLog(this::class)

}