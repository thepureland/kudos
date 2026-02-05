package io.kudos.ability.distributed.stream.common.init

import io.kudos.context.config.YamlPropertySourceFactory
import kotlinx.io.IOException
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.AnnotationMetadata
import java.util.*
import java.util.stream.Collectors

/**
 * 流式消息消费者环境注册器
 * 
 * 用于自动收集和合并Spring Cloud Function定义，支持从多个配置源加载函数定义。
 * 
 * 核心功能：
 * 1. 多源收集：从Environment默认配置和所有YAML配置文件中收集函数定义
 * 2. 定义合并：将收集到的所有函数定义合并为单个字符串，使用分号分隔
 * 3. 优先级设置：将合并后的定义注册到Environment的最前面，确保优先级最高
 * 
 * 工作流程：
 * - 从Environment中读取spring.cloud.function.definition默认值
 * - 扫描所有YAML配置文件，提取函数定义配置
 * - 将所有定义去重后合并为单个字符串
 * - 创建MapPropertySource并添加到Environment的最前面
 * 
 * 配置格式：
 * - 支持使用分号、逗号或空格分隔多个函数定义
 * - 例如："function1;function2" 或 "function1,function2"
 * 
 * 注意事项：
 * - 如果配置文件中不存在，会跳过该文件继续处理其他文件
 * - 合并后的定义会覆盖Environment中的原始配置
 */
class StreamConsumerEnvironRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {

    private lateinit var env: ConfigurableEnvironment

    override fun setEnvironment(environment: org.springframework.core.env.Environment) {
        this.env = environment as ConfigurableEnvironment
    }

    /**
     * 注册Bean定义：收集和合并Spring Cloud Function定义
     * 
     * 从多个配置源收集函数定义，合并后注册到Environment的最前面。
     * 
     * 收集来源：
     * 1. Environment默认配置：从Environment中读取spring.cloud.function.definition属性
     * 2. YAML配置文件：扫描所有YAML配置文件，提取函数定义配置
     * 
     * 处理流程：
     * 1. 获取所有YAML配置文件路径（通过YamlPropertySourceFactory）
     * 2. 从Environment读取默认配置值
     * 3. 遍历所有YAML配置文件：
     *    - 检查文件是否存在，不存在则跳过
     *    - 使用YamlPropertySourceLoader加载配置
     *    - 从每个PropertySource中提取函数定义
     * 4. 将所有定义添加到LinkedHashSet（自动去重）
     * 5. 合并为单个字符串，使用分号分隔
     * 6. 创建MapPropertySource并添加到Environment的最前面
     * 
     * 配置格式：
     * - 支持使用分号、逗号或空格分隔多个函数定义
     * - 正则表达式："[;,\\s]+"匹配分隔符
     * - 例如："function1;function2" 或 "function1,function2 function3"
     * 
     * 优先级：
     * - 合并后的定义注册到Environment的最前面（addFirst）
     * - 确保优先级最高，覆盖其他配置源的定义
     * 
     * 异常处理：
     * - 如果YAML文件加载失败，会抛出IllegalStateException
     * - 如果文件不存在，会跳过继续处理其他文件
     * 
     * @param importingClassMetadata 导入注解的元数据
     * @param registry Bean定义注册表
     */
    override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val locations = YamlPropertySourceFactory.allSourcePath()
        val loader = YamlPropertySourceLoader()

        val allDefs = java.util.LinkedHashSet<String?>()
        // 默认配置
        val defaultProperty = env.getProperty(KEY)
        if (!defaultProperty.isNullOrBlank()) {
            Arrays.stream(
                (defaultProperty).split("[;,\\s]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            )
                .filter { s: String? -> !s!!.isBlank() }
                .forEach { e: String? -> allDefs.add(e) }
        }
        // 收集定义
        for (loc in locations) {
            val res = DefaultResourceLoader().getResource(loc!!)
            if (!res.exists()) continue
            try {
                val sources = loader.load(loc, res)
                for (ps in sources) {
                    val v =
                        ps.getProperty(KEY)
                    if (v is String) {
                        Arrays.stream(
                            v.split("[;,\\s]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        )
                            .filter { s: String? -> !s!!.isBlank() }
                            .forEach { e: String? -> allDefs.add(e) }
                    }
                }
            } catch (e: IOException) {
                throw java.lang.IllegalStateException("Cannot load function definitions from $loc", e)
            }
        }

        // 合并成一条,并强制指定definition
        val merged = allDefs.stream().collect(Collectors.joining(";"))
        if (!merged.isEmpty()) {
            // 4. 注册到 Environment 最前面
            val mergedPs = MapPropertySource(
                "aggregatedFunctionDefinition",
                Collections.singletonMap<String, Any>("spring.cloud.function.definition", merged)
            )
            env.propertySources.addFirst(mergedPs)
        }
    }

    companion object {
        private const val KEY = "spring.cloud.function.definition"
    }
}
