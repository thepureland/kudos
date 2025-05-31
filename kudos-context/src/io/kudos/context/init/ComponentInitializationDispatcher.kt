package io.kudos.context.init

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
import org.springframework.stereotype.Component

/**
 * 组件初始化调度器
 *
 *  1) 在配置类自身初始化前调用 beforeInit()
 *  2) 在自身及它生产的所有 @Bean 初始化完毕后调用 afterInit()
 *  3) 不再通过 beanFactory.getBean()，而是缓存传入的实例，避免并发抢占
 *
 *  @author K
 *  @since 1.0.0
 */
@Component
open class ComponentInitializationDispatcher : SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

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

}
