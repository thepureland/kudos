package io.kudos.context.init

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component

/**
 * 组件初始化调度器
 *
 *  1) 在配置类自身初始化前调用 beforeInit()
 *  2) 在自身及它生产的所有 @Bean 初始化完毕后调用 afterInit()
 *  3) 对于每个带 @Configuration 且实现 IComponentInitializer 接口的配置类：
 *     a) 如果标注了 @AutoConfigureAfter，强制当前配置类的 BeanDefinition 依赖那些 afterClasses 的 BeanName。
 *     b) 如果标注了 @AutoConfigureBefore，强制 those beforeClasses 的 BeanDefinition 依赖当前配置类的 BeanName（即反向依赖）。
 *
 *  @author K
 *  @since 1.0.0
 */
@Component
open class ComponentInitializationDispatcher :
    BeanFactoryAware,
    SmartInstantiationAwareBeanPostProcessor,
    BeanDefinitionRegistryPostProcessor,
    Ordered {

    private lateinit var beanFactory: ConfigurableListableBeanFactory

    // key = 配置类 beanName
    private val componentBeanNames = mutableMapOf<String, MutableSet<String>>()

    private val remainingCount = mutableMapOf<String, Int>()

    // 缓存每个配置类的实例，由 Spring 传进来
    private val initializerInstances = mutableMapOf<String, IComponentInitializer>()

    private val beforeCalled = mutableSetOf<String>()
    private val afterCalled = mutableSetOf<String>()

    override fun setBeanFactory(factory: BeanFactory) {
        @Suppress("UNCHECKED_CAST")
        beanFactory = factory as ConfigurableListableBeanFactory

        beanFactory
            .getBeanNamesForType(IComponentInitializer::class.java)
            .forEach { compName ->
                // 收集 factoryBeanName == compName 的所有 Bean，再加上 compName 自身
                val children = beanFactory.beanDefinitionNames
                    .filter { name ->
                        beanFactory.getBeanDefinition(name).factoryBeanName == compName
                    }
                    .toMutableSet()
                    .apply { add(compName) }

                componentBeanNames[compName] = children
                remainingCount[compName] = children.size
            }
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (bean is IComponentInitializer && beanName !in beforeCalled) {
            // 缓存实例
            initializerInstances[beanName] = bean
            // 调用 beforeInit
            bean.beforeInit()
            beforeCalled += beanName
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        componentBeanNames.forEach { (compName, names) ->
            if (beanName in names && compName !in afterCalled) {
                val left = (remainingCount[compName] ?: 0) - 1
                remainingCount[compName] = left
                if (left == 0) {
                    // 直接从缓存里拿实例，避免再次 getBean()
                    val initializer = initializerInstances[compName]
                    initializer?.afterInit()
                    afterCalled += compName
                }
            }
        }
        return bean
    }

    override fun getEarlyBeanReference(bean: Any, beanName: String): Any {
        // 让代理也参与 postProcessAfterInitialization 的倒计数
        return postProcessAfterInitialization(bean, beanName)
    }

    override fun predictBeanType(beanClass: Class<*>, beanName: String) = beanClass

    override fun getOrder(): Int {
        // 最早执行，确保在任何 Bean 实例化之前修改所有 BeanDefinition
        return Ordered.HIGHEST_PRECEDENCE
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        // 1. 收集所有带 @Configuration 且实现 IComponentInitializer 接口的配置类
        val configCandidates = linkedMapOf<String, String>()
        for (beanName in registry.beanDefinitionNames) {
            val bd: BeanDefinition = registry.getBeanDefinition(beanName)
            val className: String? = bd.beanClassName
            if (className == null) continue
            try {
                val clazz = Class.forName(className)
                if (AnnotationUtils.findAnnotation(clazz, Configuration::class.java) != null
                    && IComponentInitializer::class.java.isAssignableFrom(clazz)
                ) {
                    configCandidates[beanName] = className
                }
            } catch (_: ClassNotFoundException) {
                // 忽略无法加载的类
            }
        }
        if (configCandidates.isEmpty()) return

        // 2. 遍历每个配置类，处理 @AutoConfigureAfter 和 @AutoConfigureBefore
        for ((currentBeanName, currentClassName) in configCandidates) {
            try {
                val currentClass = Class.forName(currentClassName)

                // 2a. 处理 @AutoConfigureAfter：当前 Bean 要依赖 afterClasses 所在配置类的 Bean
                AnnotationUtils.findAnnotation(currentClass, AutoConfigureAfter::class.java)
                    ?.let { afterAnno ->
                        val afterClasses = afterAnno.value
                        val dependsOnAfter = mutableSetOf<String>()
                        for (depClass in afterClasses) {
                            for ((candidateBeanName, candidateClassName) in configCandidates) {
                                if (candidateClassName == depClass.qualifiedName) {
                                    dependsOnAfter.add(candidateBeanName)
                                }
                            }
                        }
                        if (dependsOnAfter.isNotEmpty()) {
                            val bd: BeanDefinition = registry.getBeanDefinition(currentBeanName)
                            val oldDepends = bd.dependsOn
                            if (!oldDepends.isNullOrEmpty()) {
                                val merged = LinkedHashSet<String>()
                                merged.addAll(oldDepends.asList())
                                merged.addAll(dependsOnAfter)
                                bd.setDependsOn(*merged.toTypedArray())
                            } else {
                                bd.setDependsOn(*dependsOnAfter.toTypedArray())
                            }
                        }
                    }

                // 2b. 处理 @AutoConfigureBefore：beforeClasses 的 Bean 要依赖当前 Bean
                AnnotationUtils.findAnnotation(currentClass, AutoConfigureBefore::class.java)
                    ?.let { beforeAnno ->
                        val beforeClasses = beforeAnno.value
                        for (depClass in beforeClasses) {
                            for ((candidateBeanName, candidateClassName) in configCandidates) {
                                if (candidateClassName == depClass.qualifiedName) {
                                    // 将 currentBeanName 加到 candidateBeanName 的 dependsOn 列表
                                    val bd: BeanDefinition = registry.getBeanDefinition(candidateBeanName)
                                    val oldDepends = bd.dependsOn
                                    if (!oldDepends.isNullOrEmpty()) {
                                        val merged = LinkedHashSet<String>()
                                        merged.addAll(oldDepends.asList())
                                        merged.add(currentBeanName)
                                        bd.setDependsOn(*merged.toTypedArray())
                                    } else {
                                        bd.setDependsOn(currentBeanName)
                                    }
                                }
                            }
                        }
                    }
            } catch (_: ClassNotFoundException) {
                // 忽略无法加载的类
            }
        }
    }

}
