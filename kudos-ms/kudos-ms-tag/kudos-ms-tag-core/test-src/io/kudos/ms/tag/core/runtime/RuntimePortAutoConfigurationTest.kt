package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.core.platform.init.TagAutoConfiguration
import io.kudos.ms.tag.core.runtime.port.AttributeSourceProvider
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagMembershipStore
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class RuntimePortAutoConfigurationTest {

    private val runner = ApplicationContextRunner().withUserConfiguration(TagAutoConfiguration::class.java)

    @Test
    fun coreProvidesRdbRuntimeDefaultsButNoBusinessAttributeSource() {
        runner.run { context ->
            assertEquals("RdbAttributeStateStore", context.getBean(AttributeStateStore::class.java).javaClass.simpleName)
            assertEquals("RdbTagMembershipStore", context.getBean(TagMembershipStore::class.java).javaClass.simpleName)
            assertEquals("RdbTagAssignmentIndex", context.getBean(TagAssignmentIndex::class.java).javaClass.simpleName)
            assertEquals("JvmTagRuleEvaluator", context.getBean(TagRuleEvaluator::class.java).javaClass.simpleName)
            assertEquals("RdbRecalculationQueue", context.getBean(RecalculationQueue::class.java).javaClass.simpleName)
            assertEquals(0, context.getBeanNamesForType(AttributeSourceProvider::class.java).size)
        }
    }

    @Test
    fun embeddingApplicationCanReplaceEveryRuntimePort() {
        val attributeStateStore = proxy<AttributeStateStore>()
        val membershipStore = proxy<TagMembershipStore>()
        val assignmentIndex = proxy<TagAssignmentIndex>()
        val evaluator = proxy<TagRuleEvaluator>()
        val queue = proxy<RecalculationQueue>()

        runner
            .withBean(AttributeStateStore::class.java, { attributeStateStore })
            .withBean(TagMembershipStore::class.java, { membershipStore })
            .withBean(TagAssignmentIndex::class.java, { assignmentIndex })
            .withBean(TagRuleEvaluator::class.java, { evaluator })
            .withBean(RecalculationQueue::class.java, { queue })
            .run { context ->
                assertSame(attributeStateStore, context.getBean(AttributeStateStore::class.java))
                assertSame(membershipStore, context.getBean(TagMembershipStore::class.java))
                assertSame(assignmentIndex, context.getBean(TagAssignmentIndex::class.java))
                assertSame(evaluator, context.getBean(TagRuleEvaluator::class.java))
                assertSame(queue, context.getBean(RecalculationQueue::class.java))
            }
    }

    private inline fun <reified T> proxy(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "toString" -> "test-${T::class.simpleName}"
            "hashCode" -> System.identityHashCode(this)
            "equals" -> false
            else -> error("Test proxy must not execute ${method.name}.")
        }
    } as T
}
