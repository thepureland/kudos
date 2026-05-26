package io.kudos.ability.distributed.stream.common.init

import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.AnnotationMetadata
import java.io.IOException
import java.util.LinkedHashSet

/**
 * Stream consumer environment registrar.
 *
 * Automatically collects and merges Spring Cloud Function definitions; supports
 * loading function definitions from multiple configuration sources.
 *
 * Core features:
 * 1. Multi-source collection: gathers function definitions from the Environment
 *    defaults and all YAML configuration files.
 * 2. Definition merging: merges all collected function definitions into a single
 *    semicolon-separated string.
 * 3. Priority handling: registers the merged definition at the front of the
 *    Environment to ensure highest priority.
 *
 * Workflow:
 * - Read the default spring.cloud.function.definition from the Environment.
 * - Scan all YAML configuration files for function definition configuration.
 * - Deduplicate all definitions and merge them into a single string.
 * - Create a MapPropertySource and add it to the front of the Environment.
 *
 * Configuration format:
 * - Multiple function definitions may be separated by semicolons, commas or spaces.
 * - For example: "function1;function2" or "function1,function2".
 *
 * Notes:
 * - If a configuration file does not exist, it is skipped and processing continues.
 * - The merged definition overrides the original configuration in the Environment.
 */
class StreamConsumerEnvironRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {

    private lateinit var env: ConfigurableEnvironment

    override fun setEnvironment(environment: org.springframework.core.env.Environment) {
        this.env = environment as ConfigurableEnvironment
    }

    /**
     * Registers bean definitions: collects and merges Spring Cloud Function definitions.
     *
     * Collects function definitions from multiple configuration sources, merges them,
     * and registers them at the front of the Environment.
     *
     * Sources:
     * 1. Environment defaults: read spring.cloud.function.definition from the Environment.
     * 2. YAML configuration files: scan all YAML configuration files for function
     *    definition configuration.
     *
     * Processing:
     * 1. Obtain all YAML configuration file paths (via YamlPropertySourceFactory).
     * 2. Read the default configuration value from the Environment.
     * 3. Iterate over all YAML configuration files:
     *    - Skip the file if it does not exist.
     *    - Load it with YamlPropertySourceLoader.
     *    - Extract the function definition from each PropertySource.
     * 4. Add all definitions to a LinkedHashSet (automatic deduplication).
     * 5. Merge into a single semicolon-separated string.
     * 6. Create a MapPropertySource and add it to the front of the Environment.
     *
     * Configuration format:
     * - Multiple function definitions may be separated by semicolons, commas or spaces.
     * - Regex: "[;,\\s]+" matches the separators.
     * - For example: "function1;function2" or "function1,function2 function3".
     *
     * Priority:
     * - The merged definition is registered at the front of the Environment (addFirst).
     * - This guarantees the highest priority, overriding definitions from other sources.
     *
     * Exception handling:
     * - If a YAML file fails to load, IllegalStateException is thrown.
     * - If a file does not exist, it is skipped and processing continues.
     *
     * @param importingClassMetadata metadata of the importing annotation
     * @param registry the bean definition registry
     */
    override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val locations = YamlPropertySourceFactory.allSourcePath()
        val loader = YamlPropertySourceLoader()

        val allDefs = LinkedHashSet<String>()
        val defSplitter = Regex("[;,\\s]+")

        /**
         * Local helper: splits the raw string by separators (`;`, `,` or whitespace),
         * trims each token and adds it to the [allDefs] deduplication set. Promoted
         * to a top-level helper purely for reuse — the YAML loop below needs the same
         * splitting logic.
         */
        fun addDefinitions(raw: String) {
            defSplitter.split(raw).map { it.trim() }.filter { it.isNotEmpty() }.forEach { allDefs.add(it) }
        }

        env.getProperty(KEY)?.takeIf { it.isNotBlank() }?.let(::addDefinitions)

        for (loc in locations) {
            if (loc.isNullOrBlank()) continue
            val res = DefaultResourceLoader().getResource(loc)
            if (!res.exists()) continue
            try {
                val sources = loader.load(loc, res)
                for (ps in sources) {
                    val v = ps.getProperty(KEY)
                    if (v is String && v.isNotBlank()) {
                        addDefinitions(v)
                    }
                }
            } catch (e: IOException) {
                throw IllegalStateException("Cannot load function definitions from $loc", e)
            }
        }

        val merged = allDefs.joinToString(";")
        if (merged.isNotEmpty()) {
            val mergedPs = MapPropertySource(
                "aggregatedFunctionDefinition",
                mapOf(KEY to merged)
            )
            env.propertySources.addFirst(mergedPs)
        }
    }

    companion object {
        private const val KEY = "spring.cloud.function.definition"
    }
}
