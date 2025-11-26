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
import java.util.Arrays
import java.util.Collections
import java.util.stream.Collectors

class StreamConsumerEnvironRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {

    private lateinit var env: ConfigurableEnvironment

    override fun setEnvironment(environment: org.springframework.core.env.Environment) {
        this.env = environment as ConfigurableEnvironment
    }

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
