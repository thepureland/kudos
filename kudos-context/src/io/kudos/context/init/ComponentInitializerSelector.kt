package io.kudos.context.init

import io.kudos.base.io.ScanKit
import io.kudos.base.logger.LogFactory
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.type.AnnotationMetadata
import kotlin.reflect.KClass

/**
 * 组件初始化器选择器
 *
 * 自动导入类路径上各jar包中实现IComponentInitializer接口的配置类.
 *
 * @author K
 * @since 1.0.0
 */
open class ComponentInitializerSelector : ImportSelector {

    private val log = LogFactory.getLog(this)

    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> {
        val exclusionComponentInitializer = getExclusionComponentInitializer(importingClassMetadata)
        val classNames = mutableListOf<String>()
        val locations = listOf("io.kudos.context", "io.kudos.ability", "io.kudos.ams")
        locations.forEach { location ->
            val classes = ScanKit.findImplementations(location, IComponentInitializer::class)
            classes.filter { it !in exclusionComponentInitializer }
                .forEach { clazz -> classNames.add(clazz.qualifiedName!!) }
        }
        return classNames.toTypedArray()
    }

    protected fun getExclusionComponentInitializer(importingClassMetadata: AnnotationMetadata): List<KClass<out IComponentInitializer>> {
        val appClass = Class.forName(importingClassMetadata.className).kotlin
        val enableKudosAnno = appClass.annotations.first { it.annotationClass == EnableKudos::class } as EnableKudos
        val exclusions = enableKudosAnno.exclusions.asList()
        exclusions.forEach { log.info("${it.simpleName}通过@EnableKudos被排除初始化!") }
        return exclusions
    }

}