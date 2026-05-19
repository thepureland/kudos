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

    /** 日志器；扫描结果与排除决策仅记录到日志，不影响导入流程 */
    private val log = LogFactory.getLog(this::class)

    /**
     * 扫描三个固定根包（`io.kudos.context` / `io.kudos.ability` / `io.kudos.ms`）下所有
     * [IComponentInitializer] 实现，剔除被 `@EnableKudos(exclusions=...)` 排除的类后导入。
     *
     * @param importingClassMetadata 应用主类的注解元数据，用于读取 [EnableKudos.exclusions]
     * @return 待 Spring 导入的初始化器全限定名数组
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
     * 从应用主类的 [EnableKudos] 注解上读取需要被排除的初始化器列表。
     * 主类未带 [EnableKudos] 时返回空列表，等价于"不排除任何初始化器"。
     *
     * @param importingClassMetadata 应用主类的注解元数据
     * @return 被显式排除的初始化器类列表
     * @author K
     * @since 1.0.0
     */
    protected fun getExclusionComponentInitializer(importingClassMetadata: AnnotationMetadata): List<KClass<out IComponentInitializer>> {
        val appClass = Class.forName(importingClassMetadata.className).kotlin
        val enableKudosAnno = appClass.annotations
            .firstOrNull { it.annotationClass == EnableKudos::class } as? EnableKudos
            ?: return emptyList()
        val exclusions = enableKudosAnno.exclusions.asList()
        exclusions.forEach { log.info("${it.simpleName}通过@EnableKudos被排除初始化!") }
        return exclusions
    }

}