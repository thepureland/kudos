package io.kudos.ability.distributed.lock.common.annotations

import java.lang.annotation.Inherited
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [DistributedLock] 注解契约测试。
 *
 * 覆盖：各属性默认值、显式赋值读取、运行时保留策略、@Inherited 元注解、可标注目标（类/函数）。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DistributedLockTest {

    @DistributedLock
    private class DefaultAnnotated {
        @DistributedLock(
            key = "biz:lock",
            waitTime = 3,
            leaseTime = 60,
            throwOnFailure = false,
            lockerBeanName = "secondLocker"
        )
        fun explicit() {
        }
    }

    @Test
    fun defaults_matchDocumentedContract() {
        val anno = DefaultAnnotated::class.java.getAnnotation(DistributedLock::class.java)

        assertNotNull(anno)
        assertEquals("", anno.key)
        assertEquals(0L, anno.waitTime)
        assertEquals(20L, anno.leaseTime)
        assertTrue(anno.throwOnFailure)
        assertEquals("", anno.lockerBeanName)
    }

    @Test
    fun explicitValues_areReadableAtRuntime() {
        val anno = DefaultAnnotated::class.java.getDeclaredMethod("explicit")
            .getAnnotation(DistributedLock::class.java)

        assertNotNull(anno)
        assertEquals("biz:lock", anno.key)
        assertEquals(3L, anno.waitTime)
        assertEquals(60L, anno.leaseTime)
        assertFalse(anno.throwOnFailure)
        assertEquals("secondLocker", anno.lockerBeanName)
    }

    @Test
    fun metaAnnotations_runtimeRetainedAndInherited() {
        val clazz = DistributedLock::class.java

        assertTrue(clazz.isAnnotationPresent(Inherited::class.java))
        // getAnnotation 在运行时能取到，说明保留策略是 RUNTIME
        assertNotNull(DefaultAnnotated::class.java.getAnnotation(DistributedLock::class.java))
    }

    @DistributedLock(key = "base:lock")
    private open class AnnotatedBase

    private class SubOfAnnotatedBase : AnnotatedBase()

    @Test
    fun inherited_subclassSeesClassLevelAnnotation() {
        // @Inherited 元注解：子类未标注时也能查询到父类的类级注解
        val anno = SubOfAnnotatedBase::class.java.getAnnotation(DistributedLock::class.java)

        assertNotNull(anno)
        assertEquals("base:lock", anno.key)
    }

}
