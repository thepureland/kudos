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

    /**
     * 读取模板根目录下的指定模板文件并返回 Freemarker [Template]，输出编码强制 UTF-8。
     *
     * @param templateFileRelativePath 模板文件相对路径（相对于模板根目录）
     * @return 加载好的 Freemarker [Template]
     * @author K
     * @since 1.0.0
     */
    fun read(templateFileRelativePath: String): Template =
        newFreeMarkerConfiguration().getTemplate(templateFileRelativePath).apply {
            outputEncoding = "UTF-8"
        }

    /**
     * 构造一个 Freemarker [Configuration]，同时支持"文件系统模板"与"classpath 模板"两种来源：
     * - [URLTemplateLoader]：让用户能在外部目录改模板而无需重新打包；
     * - `setClassForTemplateLoading`：兜底加载打包进 jar 的内置模板。
     *
     * 另外把 `macro.include`（若存在）注册为 auto-include，让模板里无需手动 `<#include>` 公共宏。
     *
     * @return 初始化好的 Freemarker 配置实例
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

    /** 日志器，仅用于打印 auto-include 解析结果 */
    private val log = LogFactory.getLog(this::class)

}