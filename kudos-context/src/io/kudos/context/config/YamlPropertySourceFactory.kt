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
 * Property source factory for yml configuration files.
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
                // Merge nacos with local file for the scenario where nacos does not contain all configurations
                log.info("Loading configuration file: {0}, size={1}", propertySource, it.size)
            }
            else -> {
                log.info("Loading configuration file: {0}", propertySource)
                return propertySource
            }
        }
        val properties = loadYamlProperties(encodedResource).apply { putAll(map) }
        return PropertiesPropertySource(sourceName, properties)
    }

    /**
     * Fetch data from the bootstrap config-data, e.g. nacos.
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
     * Record the "configuration file sourceName → containing jar URI" mapping in [SOURCE_MAP], to help later diagnose
     * "which jar this configuration came from" (a typical use case is troubleshooting yml conflicts across modules).
     * When URI retrieval fails, only WARN without throwing — a logging failure should not block configuration loading.
     *
     * @param sourceName Configuration source name (usually the yml path)
     * @param encodedRes The wrapped resource
     * @author K
     * @since 1.0.0
     */
    private fun initConfigJarMap(sourceName: String?, encodedRes: EncodedResource) {
        val url = runCatching {
            // e.g. "jar:file:/…/libs/soul-foo.jar!/application.yml"
            encodedRes.resource.uri.toString()
        }.getOrElse {
            log.warn("Failed to set the config-to-jar relationship!")
            ""
        }
        SOURCE_MAP[sourceName] = url
    }


    companion object {
        private val SOURCE_MAP: MutableMap<String?, String?> = mutableMapOf()

        /** Return a read-only view to prevent external tampering with the internal map. */
        fun getSourceMap(): Map<String?, String?> = Collections.unmodifiableMap(SOURCE_MAP)

        fun allSourcePath(): List<String?> = SOURCE_MAP.keys.toList()
    }

}