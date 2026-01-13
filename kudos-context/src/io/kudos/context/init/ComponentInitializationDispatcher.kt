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

    /**
     * 设置BeanFactory并收集组件信息
     * 
     * 收集所有IComponentInitializer实现类及其创建的Bean，用于后续的初始化调度。
     * 
     * 工作流程：
     * 1. 类型转换：将BeanFactory转换为ConfigurableListableBeanFactory
     * 2. 查找组件：获取所有IComponentInitializer类型的Bean名称
     * 3. 收集子Bean：对于每个组件，收集其创建的所有Bean（通过factoryBeanName匹配）
     * 4. 记录信息：将组件名称和子Bean集合存储到componentBeanNames
     * 5. 初始化计数：设置remainingCount，用于跟踪Bean初始化进度
     * 
     * 子Bean收集：
     * - 查找所有factoryBeanName等于组件名称的Bean
     * - 将组件自身也加入到子Bean集合中
     * - 这些Bean的初始化完成后，会触发组件的afterInit
     * 
     * 计数机制：
     * - remainingCount记录每个组件还有多少子Bean未初始化
     * - 当计数减到0时，说明所有子Bean都已初始化，可以调用afterInit
     * 
     * @param factory Spring BeanFactory
     */
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

    /**
     * Bean初始化前的处理
     * 
     * 在Bean初始化前调用beforeInit方法，并缓存组件实例。
     * 
     * 工作流程：
     * 1. 类型检查：检查Bean是否为IComponentInitializer实例
     * 2. 重复检查：检查beforeInit是否已被调用（避免重复调用）
     * 3. 缓存实例：将组件实例缓存到initializerInstances
     * 4. 调用beforeInit：执行组件的beforeInit方法
     * 5. 标记已调用：将beanName添加到beforeCalled集合
     * 
     * 调用时机：
     * - 在Bean的构造函数执行后、初始化方法执行前
     * - 确保在Bean完全初始化前执行beforeInit
     * 
     * 注意事项：
     * - 每个组件的beforeInit只会被调用一次
     * - 实例会被缓存，避免后续再次getBean
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean实例
     */
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

    /**
     * Bean初始化后的处理
     * 
     * 跟踪每个组件的子Bean初始化进度，当所有子Bean初始化完成后调用afterInit。
     * 
     * 工作流程：
     * 1. 遍历所有组件：检查当前Bean是否属于某个组件的子Bean
     * 2. 更新计数：将对应组件的remainingCount减1
     * 3. 检查完成：如果计数减到0，说明所有子Bean都已初始化
     * 4. 调用afterInit：从缓存中获取组件实例，调用afterInit方法
     * 5. 标记已调用：将组件名称添加到afterCalled集合
     * 
     * 计数机制：
     * - 每个组件维护一个remainingCount，初始值为子Bean数量
     * - 每初始化一个子Bean，计数减1
     * - 当计数为0时，说明所有子Bean都已初始化完成
     * 
     * 调用时机：
     * - 在Bean的所有初始化方法执行完成后
     * - 确保所有依赖的Bean都已准备就绪
     * 
     * 注意事项：
     * - 每个组件的afterInit只会被调用一次
     * - 使用缓存的实例，避免再次getBean造成循环依赖
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean实例
     */
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

    /**
     * 获取早期Bean引用
     * 
     * 对于需要代理的Bean，确保代理也参与初始化后的计数处理。
     * 
     * 工作流程：
     * - 调用postProcessAfterInitialization处理代理Bean
     * - 确保代理Bean的初始化也被计入组件的子Bean计数
     * 
     * 使用场景：
     * - AOP代理Bean
     * - 其他需要代理的Bean
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean实例（可能是代理）
     */
    override fun getEarlyBeanReference(bean: Any, beanName: String): Any {
        // 让代理也参与 postProcessAfterInitialization 的倒计数
        return postProcessAfterInitialization(bean, beanName)
    }

    /**
     * 预测Bean类型
     * 
     * 返回Bean的实际类型，不进行任何转换。
     * 
     * @param beanClass Bean类
     * @param beanName Bean名称
     * @return Bean类
     */
    override fun predictBeanType(beanClass: Class<*>, beanName: String) = beanClass

    /**
     * 获取执行顺序
     * 
     * 返回最高优先级，确保在任何Bean实例化之前修改所有BeanDefinition。
     * 
     * @return Ordered.HIGHEST_PRECEDENCE
     */
    override fun getOrder(): Int {
        // 最早执行，确保在任何 Bean 实例化之前修改所有 BeanDefinition
        return Ordered.HIGHEST_PRECEDENCE
    }

    /**
     * 处理Bean定义注册表
     * 
     * 扫描所有配置类，处理@AutoConfigureAfter和@AutoConfigureBefore注解，设置Bean依赖关系。
     * 
     * 工作流程：
     * 1. 收集配置类：扫描所有带@Configuration且实现IComponentInitializer的类
     * 2. 处理@AutoConfigureAfter：
     *    - 查找当前配置类上的@AutoConfigureAfter注解
     *    - 将afterClasses对应的配置类BeanName添加到当前Bean的dependsOn
     *    - 确保当前配置类在afterClasses之后初始化
     * 3. 处理@AutoConfigureBefore：
     *    - 查找当前配置类上的@AutoConfigureBefore注解
     *    - 将当前BeanName添加到beforeClasses对应Bean的dependsOn
     *    - 确保beforeClasses在当前配置类之前初始化（反向依赖）
     * 
     * 依赖合并：
     * - 如果Bean已有dependsOn，会合并新旧依赖
     * - 使用LinkedHashSet去重并保持顺序
     * 
     * 初始化顺序：
     * - @AutoConfigureAfter：当前配置类在afterClasses之后初始化
     * - @AutoConfigureBefore：beforeClasses在当前配置类之前初始化
     * 
     * 注意事项：
     * - 在Bean实例化之前执行，只修改BeanDefinition
     * - 如果类无法加载，会忽略该配置类
     * - 依赖关系通过Spring的dependsOn机制实现
     * 
     * @param registry Bean定义注册表
     */
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
