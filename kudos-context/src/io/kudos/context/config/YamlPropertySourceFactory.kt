package io.kudos.context.config

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.config.YamlMapFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.io.FileNotFoundException
import java.util.*
import java.util.function.Consumer


/**
 * yml配置文件属性源工厂
 *
 * @author https://zhuanlan.zhihu.com/p/99738603
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class YamlPropertySourceFactory : PropertySourceFactory {

//    override fun createPropertySource(name: String?, resource: EncodedResource): PropertySource<*> {
//        val factory = YamlPropertiesFactoryBean()
//        factory.setResources(resource.resource)
//        factory.afterPropertiesSet()
//        val ymlProperties = factory.getObject()
//        val propertyName = name ?: resource.resource.filename!!
//        return PropertiesPropertySource(propertyName, ymlProperties!!)
//    }

    private val SOURCE_MAP: MutableMap<String?, String?> = HashMap<String?, String?>()

    private val log = LogFactory.getLog(this)

    override fun createPropertySource(s: String?, encodedResource: EncodedResource): PropertySource<*> {
        val sourceName = s ?: encodedResource.resource.filename
        initConfigJarMap(sourceName, encodedResource)
        val propertySource = loadFromConfigData(sourceName)
        var map: MutableMap<Any?, Any?> = LinkedHashMap<Any?, Any?>()
        if (propertySource != null) {
            val source = propertySource.getSource()
            if (source is MutableMap<*, *>) {
                //nacos与本地文件合并，如果nacos不包含所有配置的场景
                map = source as MutableMap<Any?, Any?>
                log.info("加载配置文件:{0},size={1}", propertySource, map.size)
            } else {
                log.info("加载配置文件:{0}", propertySource)
                return propertySource
            }
        }
        val properties = loadYamlIntoProperties(encodedResource)
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
    private fun loadFromConfigData(sourceName: String?): PropertySource<*>? {
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

    @Throws(FileNotFoundException::class)
    private fun loadYamlIntoProperties(resource: EncodedResource): Properties {
        val res = resource.resource
        try {
            val bean = YamlMapFactoryBean()
            bean.setResources(res)
            bean.afterPropertiesSet()
            val yamlRoot = bean.getObject()
            //将yaml文件的map，扁平化为properties的map，然后转换为有序的properties
            val propertiesMap = LinkedHashMap<String?, Any?>()
            flatten("", yamlRoot!!, propertiesMap)
            val properties = OrderProperties()
            //转换为有序的properties
            propertiesMap.entries.forEach(Consumer { entry: MutableMap.MutableEntry<String?, Any?>? ->
                properties.put(entry!!.key, if (entry.value == null) "" else entry.value)
            })
            return properties
        } catch (e: Exception) {
            log.warn("加载配置文件失败！file={0},如果配置了nacos可以忽略此信息。", resource.getResource().getFilename())
            return Properties()
        }
    }

    /**
     * 将map扁平化得到有顺序的Map
     *
     * @param prefix
     * @param curr
     * @param collector
     */
    private fun flatten(prefix: String, curr: MutableMap<String?, Any?>, collector: LinkedHashMap<String?, Any?>) {
        for (entry in curr.entries) {
            val key: String = (if (prefix.isEmpty()) entry.key else prefix + "." + entry.key)!!
            val `val` = entry.value
            if (`val` is MutableMap<*, *>) {
                flatten(key, `val` as MutableMap<String?, Any?>, collector)
            } else {
                collector.put(key, `val`)
            }
        }
    }

    private fun initConfigJarMap(sourceName: String?, encodedRes: EncodedResource) {
        var url: String? = ""
        try {
            val res = encodedRes.resource
            val uri = res.uri // 例如 "jar:file:/…/libs/soul-foo.jar!/application.yml"
            url = uri.toString()
        } catch (e: Exception) {
            log.warn("设置config和jar关系失败！")
        }
        SOURCE_MAP.put(sourceName, url)
    }

    fun getSourceMap(): MutableMap<String?, String?> {
        return SOURCE_MAP
    }

    fun allSourcePath(): MutableList<String?> {
        return SOURCE_MAP.keys.stream().toList()
    }

}