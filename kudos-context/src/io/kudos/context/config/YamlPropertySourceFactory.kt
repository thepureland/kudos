package io.kudos.context.config

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.util.Collections
import java.util.Properties
import java.util.ServiceLoader


/**
 * yml配置文件属性源工厂
 *
 * @author https://zhuanlan.zhihu.com/p/99738603
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class YamlPropertySourceFactory : PropertySourceFactory {

    private val log = LogFactory.getLog(this::class)

    override fun createPropertySource(name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val sourceName = name ?: encodedResource.resource.filename ?: "application"
        initConfigJarMap(sourceName, encodedResource)
        val propertySource = loadFromConfigCenter(sourceName)
        @Suppress("UNCHECKED_CAST")
        val map: MutableMap<Any?, Any?> = when (val source = propertySource?.getSource()) {
            null -> mutableMapOf()
            is MutableMap<*, *> -> (source as MutableMap<Any?, Any?>).also {
                //nacos与本地文件合并，如果nacos不包含所有配置的场景
                log.info("加载配置文件:{0},size={1}", propertySource, it.size)
            }
            else -> {
                log.info("加载配置文件:{0}", propertySource)
                return propertySource
            }
        }
        val properties = loadYamlProperties(encodedResource).apply { putAll(map) }
        return PropertiesPropertySource(sourceName, properties)
    }

    /**
     * 从启动的config-data获取数据，比如nacos
     *
     * @param sourceName
     */
    private fun loadFromConfigCenter(sourceName: String?): PropertySource<*>? =
        ServiceLoader.load(IConfigDataFinder::class.java).firstNotNullOfOrNull { it.findConfigData(sourceName) }

    private fun loadYamlProperties(resource: EncodedResource): Properties {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(resource.resource)
        factory.afterPropertiesSet()
        return requireNotNull(factory.getObject()) { "YAML properties could not be loaded" }
    }

    /**
     * 把"配置文件 sourceName → 所在 jar URI"对应关系记到 [SOURCE_MAP]，
     * 便于后续诊断"配置来自哪个 jar"（典型用途：多模块 yml 冲突排查）。
     * URI 获取失败时只 WARN 不抛异常——记录失败不应阻断配置加载本身。
     *
     * @param sourceName 配置源名（通常是 yml 路径）
     * @param encodedRes 包装好的资源
     * @author K
     * @since 1.0.0
     */
    private fun initConfigJarMap(sourceName: String?, encodedRes: EncodedResource) {
        val url = runCatching {
            // 例如 "jar:file:/…/libs/soul-foo.jar!/application.yml"
            encodedRes.resource.uri.toString()
        }.getOrElse {
            log.warn("设置config和jar关系失败！")
            ""
        }
        SOURCE_MAP[sourceName] = url
    }


    companion object {
        private val SOURCE_MAP: MutableMap<String?, String?> = mutableMapOf()

        /** 返回只读视图，避免外部篡改内部映射。 */
        fun getSourceMap(): Map<String?, String?> = Collections.unmodifiableMap(SOURCE_MAP)

        fun allSourcePath(): List<String?> = SOURCE_MAP.keys.toList()
    }

}