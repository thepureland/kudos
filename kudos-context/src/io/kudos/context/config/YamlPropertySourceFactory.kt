package io.kudos.context.config

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.util.*


/**
 * yml配置文件属性源工厂
 *
 * @author https://zhuanlan.zhihu.com/p/99738603
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class YamlPropertySourceFactory : PropertySourceFactory {

    private val log = LogFactory.getLog(this)

    override fun createPropertySource(name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val sourceName = name ?: encodedResource.resource.filename!!
        initConfigJarMap(sourceName, encodedResource)
        val propertySource = loadFromConfigCenter(sourceName)
        var map = mutableMapOf<Any?, Any?>()
        if (propertySource != null) {
            val source = propertySource.getSource()
            if (source is MutableMap<*, *>) {
                //nacos与本地文件合并，如果nacos不包含所有配置的场景
                @Suppress("UNCHECKED_CAST")
                map = source as MutableMap<Any?, Any?>
                log.info("加载配置文件:{0},size={1}", propertySource, map.size)
            } else {
                log.info("加载配置文件:{0}", propertySource)
                return propertySource
            }
        }
        val properties = loadYamlProperties(encodedResource)
        for (e in map.entries) {
            properties.put(e.key, e.value)
        }
        return PropertiesPropertySource(sourceName, properties)
    }

    /**
     * 从启动的config-data获取数据，比如nacos
     *
     * @param sourceName
     */
    private fun loadFromConfigCenter(sourceName: String?): PropertySource<*>? {
        val configDataFinders = ServiceLoader.load(IConfigDataFinder::class.java)
        if (configDataFinders != null) {
            for (configDataFinder in configDataFinders) {
                val configData: PropertySource<*>? = configDataFinder.findConfigData(sourceName)
                if (configData != null) {
                    return configData
                }
            }
        }
        return null
    }

    private fun loadYamlProperties(resource: EncodedResource): Properties {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(resource.resource)
        factory.afterPropertiesSet()
        val ymlProperties = factory.getObject()
        return ymlProperties!!
    }

    private fun initConfigJarMap(sourceName: String?, encodedRes: EncodedResource) {
        var url: String? = ""
        try {
            val res = encodedRes.resource
            val uri = res.uri // 例如 "jar:file:/…/libs/soul-foo.jar!/application.yml"
            url = uri.toString()
        } catch (_: Exception) {
            log.warn("设置config和jar关系失败！")
        }
        SOURCE_MAP.put(sourceName, url)
    }


    companion object {
        private val SOURCE_MAP: MutableMap<String?, String?> = HashMap<String?, String?>()

        fun getSourceMap(): MutableMap<String?, String?> {
            return SOURCE_MAP
        }

        fun allSourcePath(): MutableList<String?> {
            return SOURCE_MAP.keys.stream().toList()
        }
    }

}