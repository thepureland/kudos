package io.kudos.context.init

import io.kudos.base.io.ScanKit
import io.kudos.base.logger.LogFactory
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.type.AnnotationMetadata
import kotlin.reflect.KClass

/**
 * Component initializer selector.
 *
 * Automatically imports configuration classes that implement IComponentInitializer from jars on the classpath.
 *
 * @author K
 * @since 1.0.0
 */
open class ComponentInitializerSelector : ImportSelector {

    /** Logger; scan results and exclusion decisions are only recorded in logs and do not affect the import flow */
    private val log = LogFactory.getLog(this::class)

    /**
     * Scan all [IComponentInitializer] implementations under the three fixed root packages
     * (`io.kudos.context` / `io.kudos.ability` / `io.kudos.ms`), drop the classes excluded by
     * `@EnableKudos(exclusions=...)`, and import the rest.
     *
     * @param importingClassMetadata Annotation metadata of the application's main class, used to read
     *   [EnableKudos.exclusions]
     * @return An array of fully-qualified initializer names for Spring to import
     * @author K
     * @since 1.0.0
     */
    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> {
        val exclusions = getExclusionComponentInitializer(importingClassMetadata)
        return listOf("io.kudos.context", "io.kudos.ability", "io.kudos.ms")
            .flatMap { location -> ScanKit.findImplementations(location, IComponentInitializer::class) }
            .filterNot { it in exclusions }
            .map { it.qualifiedName ?: it.simpleName ?: "Unknown" }
            .toTypedArray()
    }

    /**
     * Read the list of initializers to be excluded from the application main class's [EnableKudos] annotation.
     * If the main class is not annotated with [EnableKudos], return an empty list, equivalent to "no initializers
     * excluded".
     *
     * @param importingClassMetadata Annotation metadata of the application's main class
     * @return The list of initializer classes explicitly excluded
     * @author K
     * @since 1.0.0
     */
    protected fun getExclusionComponentInitializer(importingClassMetadata: AnnotationMetadata): List<KClass<out IComponentInitializer>> {
        val appClass = Class.forName(importingClassMetadata.className).kotlin
        val enableKudosAnno = appClass.annotations
            .firstOrNull { it.annotationClass == EnableKudos::class } as? EnableKudos
            ?: return emptyList()
        val exclusions = enableKudosAnno.exclusions.asList()
        exclusions.forEach { log.info("${it.simpleName} initialization is excluded via @EnableKudos!") }
        return exclusions
    }

}