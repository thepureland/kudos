package io.kudos.context.init

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
import org.springframework.beans.factory.getBeanNamesForType
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component

/**
 * Component initialization dispatcher.
 *
 *  1) Call beforeInit() before the configuration class itself is initialized.
 *  2) Call afterInit() after the configuration class itself and all @Bean instances it produces are initialized.
 *  3) For each configuration class annotated with @Configuration that implements IComponentInitializer:
 *     a) If annotated with @AutoConfigureAfter, force the current configuration class's BeanDefinition to depend on
 *        the BeanNames of those afterClasses.
 *     b) If annotated with @AutoConfigureBefore, force the BeanDefinitions of those beforeClasses to depend on the
 *        current configuration class's BeanName (i.e. reverse dependency).
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

    /**
     * key = configuration class beanName; value = the set of child beans (plus the component itself) that have
     * **not yet** finished initialization. Shrinks as beans complete; when it becomes empty, afterInit fires.
     *
     * Set-removal (instead of the previous "fixed child set + remaining counter" pair) is deliberately idempotent:
     * the same beanName may flow through both [getEarlyBeanReference] (circular-dependency early proxy exposure)
     * and [postProcessAfterInitialization] — with a counter that meant a double decrement and a prematurely
     * triggered afterInit; removing from a set counts each bean exactly once no matter how many paths it takes.
     */
    private val remainingBeanNames = mutableMapOf<String, MutableSet<String>>()

    // Cache an instance of each configuration class, passed in by Spring
    private val initializerInstances = mutableMapOf<String, IComponentInitializer>()

    private val beforeCalled = mutableSetOf<String>()
    private val afterCalled = mutableSetOf<String>()

    /**
     * Set the BeanFactory and collect component information.
     *
     * Collects all IComponentInitializer implementations and the beans they create, for use in subsequent
     * initialization scheduling.
     *
     * Workflow:
     * 1. Type cast: cast BeanFactory to ConfigurableListableBeanFactory.
     * 2. Find components: get all bean names of type IComponentInitializer.
     * 3. Collect child beans: for each component, collect all beans it creates (matched by factoryBeanName).
     * 4. Record information: store the component name and child bean set in [remainingBeanNames].
     *
     * Child bean collection:
     * - Look up all beans whose factoryBeanName equals the component name.
     * - Add the component itself to the child bean set.
     * - When these beans finish initializing, the component's afterInit is triggered.
     *
     * Tracking mechanism:
     * - [remainingBeanNames] holds, per component, the set of beans not yet initialized.
     * - Each bean is removed from the set once it finishes initializing (idempotently).
     * - When the set becomes empty, all child beans have finished and afterInit can be invoked.
     *
     * @param factory The Spring BeanFactory
     */
    override fun setBeanFactory(factory: BeanFactory) {
        @Suppress("UNCHECKED_CAST")
        beanFactory = factory as ConfigurableListableBeanFactory

        beanFactory
            .getBeanNamesForType<IComponentInitializer>()
            .forEach { compName ->
                // Collect all beans whose factoryBeanName == compName, plus compName itself
                val children = beanFactory.beanDefinitionNames
                    .filter { name ->
                        beanFactory.getBeanDefinition(name).factoryBeanName == compName
                    }
                    .toMutableSet()
                    .apply { add(compName) }

                remainingBeanNames[compName] = children
            }
    }

    /**
     * Pre-initialization processing for a bean.
     *
     * Calls beforeInit before the bean is initialized and caches the component instance.
     *
     * Workflow:
     * 1. Type check: check whether the bean is an IComponentInitializer.
     * 2. Duplicate check: check whether beforeInit has already been called (avoid duplicate invocation).
     * 3. Cache the instance: cache the component instance into initializerInstances.
     * 4. Invoke beforeInit: execute the component's beforeInit method.
     * 5. Mark as called: add the beanName to the beforeCalled set.
     *
     * Invocation timing:
     * - After the bean's constructor runs and before its initialization methods run.
     * - Ensure beforeInit executes before the bean is fully initialized.
     *
     * Notes:
     * - Each component's beforeInit is called only once.
     * - The instance is cached to avoid calling getBean again later.
     *
     * @param bean The bean instance
     * @param beanName The bean name
     * @return The processed bean instance
     */
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (bean is IComponentInitializer && beanName !in beforeCalled) {
            // Cache the instance
            initializerInstances[beanName] = bean
            // Invoke beforeInit
            bean.beforeInit()
            beforeCalled += beanName
        }
        return bean
    }

    /**
     * Post-initialization processing for a bean.
     *
     * Tracks each component's child-bean initialization progress, and calls afterInit once all child beans are
     * initialized.
     *
     * Workflow:
     * 1. Iterate over all components: remove the current bean from each component's remaining-bean set.
     * 2. Completion check: if a set becomes empty, all of that component's child beans are initialized.
     * 3. Invoke afterInit: get the component instance from the cache and call afterInit.
     * 4. Mark as called: add the component name to the afterCalled set.
     *
     * Tracking mechanism:
     * - Each component maintains a remaining-bean set initialized to all its child beans (plus itself).
     * - Each time a child bean finishes initializing, it is removed from the set.
     * - `Set.remove` is idempotent: a beanName entering twice (e.g. via [getEarlyBeanReference] **and** this
     *   method during circular-dependency proxying) is counted only once, so afterInit cannot fire prematurely.
     * - When the set becomes empty, all child beans have finished initializing.
     *
     * Invocation timing:
     * - After all of the bean's initialization methods have finished executing.
     * - Ensures every dependency bean is ready.
     *
     * Notes:
     * - Each component's afterInit is called only once.
     * - The cached instance is used to avoid getBean causing a circular dependency.
     *
     * @param bean The bean instance
     * @param beanName The bean name
     * @return The processed bean instance
     */
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        remainingBeanNames.forEach { (compName, names) ->
            if (compName !in afterCalled && names.remove(beanName) && names.isEmpty()) {
                // Fetch the instance directly from the cache to avoid calling getBean() again
                initializerInstances[compName]?.afterInit()
                afterCalled += compName
            }
        }
        return bean
    }

    /**
     * Obtain an early bean reference (e.g. during circular dependency resolution).
     *
     * Forwards to [postProcessAfterInitialization] so that proxy objects also participate in the child-bean
     * completion tracking. Since tracking is based on idempotent set removal, the same beanName entering both this
     * path and the plain initialization path is counted exactly once — the historical "double decrement causing
     * premature afterInit" issue with the counter-based implementation no longer applies.
     *
     * @param bean The bean instance
     * @param beanName The bean name
     * @return The processed bean instance (possibly a proxy)
     */
    override fun getEarlyBeanReference(bean: Any, beanName: String): Any {
        // Let the proxy also participate in postProcessAfterInitialization's completion tracking
        return postProcessAfterInitialization(bean, beanName)
    }

    /**
     * Predict bean type.
     *
     * Return the bean's actual type without any conversion.
     *
     * @param beanClass The bean class
     * @param beanName The bean name
     * @return The bean class
     */
    override fun predictBeanType(beanClass: Class<*>, beanName: String) = beanClass

    /**
     * Get execution order.
     *
     * Returns the highest precedence to ensure all BeanDefinitions are modified before any beans are instantiated.
     *
     * @return Ordered.HIGHEST_PRECEDENCE
     */
    // Execute earliest to ensure all BeanDefinitions are modified before any bean instantiation
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    /**
     * Process the bean definition registry.
     *
     * Scan all configuration classes, process @AutoConfigureAfter and @AutoConfigureBefore annotations, and set bean
     * dependencies.
     *
     * Workflow:
     * 1. Collect configuration classes: scan all classes annotated with @Configuration that implement
     *    IComponentInitializer.
     * 2. Process @AutoConfigureAfter:
     *    - Look up the @AutoConfigureAfter annotation on the current configuration class.
     *    - Add the bean names of the configuration classes corresponding to afterClasses to the current bean's
     *      dependsOn.
     *    - Ensure the current configuration class is initialized after afterClasses.
     * 3. Process @AutoConfigureBefore:
     *    - Look up the @AutoConfigureBefore annotation on the current configuration class.
     *    - Add the current bean name to the dependsOn of the beans corresponding to beforeClasses.
     *    - Ensure beforeClasses are initialized before the current configuration class (reverse dependency).
     *
     * Dependency merging:
     * - If a bean already has dependsOn, the old and new dependencies are merged.
     * - LinkedHashSet is used to deduplicate while preserving order.
     *
     * Initialization order:
     * - @AutoConfigureAfter: the current configuration class is initialized after afterClasses.
     * - @AutoConfigureBefore: beforeClasses are initialized before the current configuration class.
     *
     * Notes:
     * - Runs before any bean instantiation and only modifies BeanDefinitions.
     * - If a class fails to load, the configuration class is ignored.
     * - Dependencies are implemented via Spring's dependsOn mechanism.
     *
     * @param registry The bean definition registry
     */
    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        // 1. Collect all configuration classes annotated with @Configuration that implement IComponentInitializer
        val configCandidates = registry.beanDefinitionNames
            .mapNotNull { beanName ->
                val className = registry.getBeanDefinition(beanName).beanClassName ?: return@mapNotNull null
                val clazz = runCatching { Class.forName(className) }.getOrNull() ?: return@mapNotNull null
                if (AnnotationUtils.findAnnotation(clazz, Configuration::class.java) != null
                    && IComponentInitializer::class.java.isAssignableFrom(clazz)
                ) beanName to className else null
            }
            .toMap(linkedMapOf())
        if (configCandidates.isEmpty()) return

        // 2. Iterate over each configuration class and process @AutoConfigureAfter and @AutoConfigureBefore
        for ((currentBeanName, currentClassName) in configCandidates) {
            val currentClass = runCatching { Class.forName(currentClassName) }.getOrNull() ?: continue

            // 2a. Handle @AutoConfigureAfter: the current bean must depend on beans of the afterClasses
            AnnotationUtils.findAnnotation(currentClass, AutoConfigureAfter::class.java)?.let { afterAnno ->
                val dependsOnAfter = afterAnno.value.flatMap { depClass ->
                    configCandidates.filterValues { it == depClass.qualifiedName }.keys
                }.toSet()
                if (dependsOnAfter.isNotEmpty()) {
                    appendDependsOn(registry.getBeanDefinition(currentBeanName), dependsOnAfter)
                }
            }

            // 2b. Handle @AutoConfigureBefore: beans of beforeClasses must depend on the current bean
            AnnotationUtils.findAnnotation(currentClass, AutoConfigureBefore::class.java)?.let { beforeAnno ->
                beforeAnno.value.forEach { depClass ->
                    configCandidates
                        .filterValues { it == depClass.qualifiedName }
                        .keys
                        .forEach { candidateBeanName ->
                            // Add currentBeanName to candidateBeanName's dependsOn list
                            appendDependsOn(registry.getBeanDefinition(candidateBeanName), setOf(currentBeanName))
                        }
                }
            }
        }
    }

    /** Merge [newDepends] into [bd]'s existing `dependsOn`, preserving order and deduplicating. */
    private fun appendDependsOn(bd: BeanDefinition, newDepends: Set<String>) {
        val merged = LinkedHashSet<String>().apply {
            bd.dependsOn?.let { addAll(it.asList()) }
            addAll(newDepends)
        }
        bd.setDependsOn(*merged.toTypedArray())
    }

}
