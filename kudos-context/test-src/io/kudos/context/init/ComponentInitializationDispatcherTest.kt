package io.kudos.context.init

import io.kudos.contexttest.fixture.CfgA
import io.kudos.contexttest.fixture.CfgB
import io.kudos.contexttest.fixture.CfgC
import io.kudos.contexttest.fixture.CfgE
import io.kudos.contexttest.fixture.CfgNotInitializer
import io.kudos.contexttest.fixture.PlainInitializer
import io.kudos.contexttest.fixture.RecordingInitializer
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.core.Ordered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for ComponentInitializationDispatcher
 *
 * Coverage (pure unit, driven by a DefaultListableBeanFactory — no Spring Boot container):
 * - setBeanFactory: child-bean collection by factoryBeanName (plus the component itself)
 * - postProcessBeforeInitialization: beforeInit fired exactly once, instance cached, non-initializer passthrough
 * - postProcessAfterInitialization: afterInit fires only after ALL children complete; idempotent
 *   set-removal (the same beanName completing twice counts once); afterCalled guard
 * - getEarlyBeanReference: forwards to the same idempotent tracking (no premature afterInit)
 * - predictBeanType / getOrder trivial contracts
 * - postProcessBeanDefinitionRegistry: @AutoConfigureAfter forward dependency, @AutoConfigureBefore
 *   reverse dependency, dependsOn merging with pre-existing entries, unresolvable/missing class name
 *   skipping, non-candidate filtering, and the empty-candidate early return
 *
 * @author K
 * @since 1.0.0
 */
internal class ComponentInitializationDispatcherTest {

    // ============================================================
    // Lifecycle dispatch: beforeInit / afterInit
    // ============================================================

    private fun newTrackingSetup(): Triple<ComponentInitializationDispatcher, DefaultListableBeanFactory, RecordingInitializer> {
        val dispatcher = ComponentInitializationDispatcher()
        val bf = DefaultListableBeanFactory()
        bf.registerBeanDefinition("comp", RootBeanDefinition(RecordingInitializer::class.java))
        val child = RootBeanDefinition(StringBuilder::class.java)
        child.factoryBeanName = "comp"
        bf.registerBeanDefinition("childBean", child)
        dispatcher.setBeanFactory(bf)
        return Triple(dispatcher, bf, RecordingInitializer())
    }

    @Test
    fun beforeInitFiresExactlyOnceAndCachesInstance() {
        val (dispatcher, _, initializer) = newTrackingSetup()
        assertSame(initializer, dispatcher.postProcessBeforeInitialization(initializer, "comp"))
        dispatcher.postProcessBeforeInitialization(initializer, "comp")
        assertEquals(1, initializer.beforeCount, "beforeInit must be invoked exactly once per component")
    }

    @Test
    fun nonInitializerBeanPassesThroughBeforeInitialization() {
        val (dispatcher, _, _) = newTrackingSetup()
        val plain = Any()
        assertSame(plain, dispatcher.postProcessBeforeInitialization(plain, "someBean"))
    }

    @Test
    fun afterInitFiresOnlyWhenAllChildrenComplete() {
        val (dispatcher, _, initializer) = newTrackingSetup()
        dispatcher.postProcessBeforeInitialization(initializer, "comp")

        // Only the child completed -> not yet
        dispatcher.postProcessAfterInitialization("any", "childBean")
        assertEquals(0, initializer.afterCount, "afterInit must wait for the component itself")

        // The component completed too -> fires
        dispatcher.postProcessAfterInitialization(initializer, "comp")
        assertEquals(1, initializer.afterCount)

        // Re-entry after completion must not re-fire (afterCalled guard)
        dispatcher.postProcessAfterInitialization("any", "childBean")
        dispatcher.postProcessAfterInitialization(initializer, "comp")
        assertEquals(1, initializer.afterCount, "afterInit must never fire twice")
    }

    @Test
    fun earlyBeanReferenceIsIdempotentWithAfterInitialization() {
        val (dispatcher, _, initializer) = newTrackingSetup()
        dispatcher.postProcessBeforeInitialization(initializer, "comp")

        // The same child flows through BOTH the early-proxy path and the normal path
        val earlyRef = dispatcher.getEarlyBeanReference("childInstance", "childBean")
        assertEquals("childInstance", earlyRef)
        dispatcher.postProcessAfterInitialization("childInstance", "childBean")
        assertEquals(0, initializer.afterCount, "double-counting a child must not fire afterInit prematurely")

        dispatcher.postProcessAfterInitialization(initializer, "comp")
        assertEquals(1, initializer.afterCount)
    }

    @Test
    fun predictBeanTypeReturnsGivenClassAndOrderIsHighest() {
        val dispatcher = ComponentInitializationDispatcher()
        assertEquals(String::class.java, dispatcher.predictBeanType(String::class.java, "x"))
        assertEquals(Ordered.HIGHEST_PRECEDENCE, dispatcher.order)
    }

    // ============================================================
    // postProcessBeanDefinitionRegistry: dependsOn wiring
    // ============================================================

    private fun newRegistry(): DefaultListableBeanFactory {
        val registry = DefaultListableBeanFactory()
        registry.registerBeanDefinition("cfgA", RootBeanDefinition(CfgA::class.java))
        registry.registerBeanDefinition("cfgB", RootBeanDefinition(CfgB::class.java))
        registry.registerBeanDefinition("cfgC", RootBeanDefinition(CfgC::class.java))
        registry.registerBeanDefinition("cfgE", RootBeanDefinition(CfgE::class.java))
        registry.registerBeanDefinition("cfgNotInit", RootBeanDefinition(CfgNotInitializer::class.java))
        registry.registerBeanDefinition("plainInit", RootBeanDefinition(PlainInitializer::class.java))
        // beanClassName == null -> skipped by the candidate scan
        registry.registerBeanDefinition("noClassName", GenericBeanDefinition().apply { factoryBeanName = "cfgB" })
        // unloadable class -> skipped by the candidate scan
        registry.registerBeanDefinition(
            "badClassName",
            GenericBeanDefinition().apply { setBeanClassName("no.such.pkg.MissingClass") }
        )
        return registry
    }

    @Test
    fun autoConfigureAfterAddsForwardDependsOn() {
        val registry = newRegistry()
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        val dependsOn = registry.getBeanDefinition("cfgA").dependsOn!!.toList()
        assertEquals(listOf("cfgB"), dependsOn, "@AutoConfigureAfter(CfgB) means cfgA depends on cfgB")
    }

    @Test
    fun autoConfigureBeforeAddsReverseDependsOn() {
        val registry = newRegistry()
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        val dependsOn = registry.getBeanDefinition("cfgB").dependsOn!!.toList()
        assertTrue("cfgC" in dependsOn, "@AutoConfigureBefore(CfgB) on cfgC means cfgB depends on cfgC")
    }

    @Test
    fun dependsOnMergesWithExistingEntriesPreservingOrder() {
        val registry = newRegistry()
        registry.getBeanDefinition("cfgA").setDependsOn("preExisting", "cfgB")
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        assertEquals(
            listOf("preExisting", "cfgB"),
            registry.getBeanDefinition("cfgA").dependsOn!!.toList(),
            "existing dependsOn must be preserved and deduplicated"
        )
    }

    @Test
    fun afterAnnotationPointingAtUnregisteredClassAddsNothing() {
        val registry = newRegistry()
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        assertNull(registry.getBeanDefinition("cfgE").dependsOn, "an unmatched afterClass must not produce dependsOn")
    }

    @Test
    fun nonCandidatesAreLeftUntouched() {
        val registry = newRegistry()
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        assertNull(registry.getBeanDefinition("cfgNotInit").dependsOn)
        assertNull(registry.getBeanDefinition("plainInit").dependsOn)
        assertNull(registry.getBeanDefinition("noClassName").dependsOn)
        assertNull(registry.getBeanDefinition("badClassName").dependsOn)
    }

    @Test
    fun emptyCandidateRegistryReturnsEarlyWithoutError() {
        val registry = DefaultListableBeanFactory()
        registry.registerBeanDefinition("plain", RootBeanDefinition(StringBuilder::class.java))
        ComponentInitializationDispatcher().postProcessBeanDefinitionRegistry(registry)
        assertNull(registry.getBeanDefinition("plain").dependsOn)
    }
}
