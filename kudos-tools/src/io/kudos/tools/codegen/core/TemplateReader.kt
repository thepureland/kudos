package io.kudos.tools.codegen.core

import freemarker.cache.MultiTemplateLoader
import freemarker.cache.URLTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import io.kudos.base.logger.LogFactory
import java.net.URI
import java.net.URL


/**
 * 模板内容读取器
 *
 * @author K
 * @since 1.0.0
 */
class TemplateReader {

    fun read(templateFileRelativePath: String) : Template {
        val template = newFreeMarkerConfiguration().getTemplate(templateFileRelativePath)
        template.outputEncoding = "UTF-8"
        return template
    }

    private fun newFreeMarkerConfiguration(): Configuration {
        val templateRootDir = CodeGeneratorContext.config.getTemplateInfo().rootDir
        val root = URI("file:$templateRootDir").toURL()
        val multiTemplateLoader = MultiTemplateLoader(arrayOf(
            object : URLTemplateLoader() {
                override fun getURL(template: String): URL = root.toURI().resolve(template).toURL()
            }
        ))
        val conf = Configuration(Configuration.VERSION_2_3_30)
        conf.templateLoader = multiTemplateLoader
        conf.numberFormat = "###############"
        conf.booleanFormat = "true,false"
        conf.defaultEncoding = "UTF-8"
        conf.setClassForTemplateLoading(
            CodeGenerator::class.java,
            "/templates/${CodeGeneratorContext.config.getTemplateInfo().name}"
        )
        val autoIncludes = listOf("macro.include")
        val availableAutoInclude = FreemarkerKit.getAvailableAutoInclude(conf, autoIncludes)
        conf.setAutoIncludes(availableAutoInclude)
        log.debug("set Freemarker.autoIncludes:$availableAutoInclude for templateName:$templateRootDir autoIncludes:$autoIncludes")
        return conf
    }

    private val log = LogFactory.getLog(this)

}