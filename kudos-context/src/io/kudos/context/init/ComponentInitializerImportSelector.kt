package io.kudos.context.init

import io.kudos.base.io.ScanKit
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.type.AnnotationMetadata

/**
 * 自动配置类导入选择器
 *
 * 自动导入类路径上各jar包中init package中以AutoConfiguration结尾的配置类.
 *
 * @author K
 * @since 1.0.0
 */
open class ComponentInitializerImportSelector : ImportSelector {

    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> {
        val classNames = mutableListOf<String>()
        val locations = listOf("io.kudos.ability", "io.kudos.ms")
        locations.forEach { location ->
            val classes = ScanKit.findImplementations(location, IComponentInitializer::class)
            classes.forEach { clazz -> classNames.add(clazz.qualifiedName!!) }
        }
        return classNames.toTypedArray()
    }

}