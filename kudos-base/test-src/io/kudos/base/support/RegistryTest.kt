package io.kudos.base.support

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue


/**
 * test for Registry
 *
 * @since ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class RegistryTest {

    @Test
    fun testLookup_onEmptyKey_returnsEmptyListAndDoesNotModifyRegistry() {
        // 使用一个从未注册过的 key
        val key = "uniqueKey_empty"
        val list1 = Registry.lookup(key)
        // 第一次 lookup，应该是空列表
        assertTrue(list1.isEmpty(), "对不存在的 key 调用 lookup，应返回空列表")
        // 再次 lookup，map 内部依然为空
        val list2 = Registry.lookup(key)
        assertTrue(list2.isEmpty(), "对不存在 key 的 lookup 不应改变 Registry 内部状态")
    }

    @Test
    fun testRegister_singleObject_andNoDuplicate() {
        val key = "testKey_single"
        val obj = Any()

        // 初次 lookup 仍然为空
        assertTrue(Registry.lookup(key).isEmpty(), "初次 lookup，新 key 应为空")

        // 第一次注册
        Registry.register(key, obj)
        val afterFirst = Registry.lookup(key)
        assertEquals(1, afterFirst.size, "注册一个对象后，lookup 应返回 size = 1")
        assertTrue(afterFirst.contains(obj), "lookup 列表中应包含刚才注册的 obj")

        // 第二次注册同一个 obj，不应重复添加
        Registry.register(key, obj)
        val afterSecond = Registry.lookup(key)
        assertEquals(1, afterSecond.size, "第二次注册同一个对象后，列表长度仍应为 1，不应重复")
        assertTrue(afterSecond.contains(obj), "lookup 列表仍应包含 obj")
    }

    @Test
    fun testRegister_multipleObjects_andAllowDuplicatesInBulk() {
        val key = "testKey_bulk"
        val obj1 = "A"
        val obj2 = 123
        val obj3 = "A" // 与 obj1 相同的引用或 equals，但 Bulk 注册时不去重

        // 批量注册空数组，应什么都不做
        Registry.register(key /*key*/, *emptyArray<Any>())
        assertTrue(Registry.lookup(key).isEmpty(), "批量注册空 vararg，不应改变内部状态")

        // 第一次批量注册 obj1、obj2
        Registry.register(key, obj1, obj2)
        val afterBulk1 = Registry.lookup(key)
        assertEquals(2, afterBulk1.size, "批量注册两个对象后，lookup size 应为 2")
        assertTrue(afterBulk1.contains(obj1) && afterBulk1.contains(obj2),
            "lookup 列表应包含 obj1 和 obj2")

        // 再次批量注册 obj3（与 obj1 equals）和 obj2（与之前相同）
        Registry.register(key, obj3, obj2)
        val afterBulk2 = Registry.lookup(key)
        // Bulk 方法没有去重逻辑，所以列表会累加
        assertEquals(4, afterBulk2.size, "批量注册不去重，应累加到 4 个元素")
        // 可以检查对应位置是否正确插入
        // - index 0: obj1
        // - index 1: obj2
        // - index 2: obj3 (equals obj1)
        // - index 3: obj2
        assertEquals(listOf(obj1, obj2, obj3, obj2), afterBulk2,
            "Bulk 注册后，列表顺序与传入顺序一致，且允许重复")
    }

    @Test
    fun testLookup_afterMixOfSingleAndBulkRegistration() {
        val key = "testKey_mixed"
        val obj1 = "X"
        val obj2 = "Y"
        val obj3 = "X"  // equals obj1

        // 单个注册 obj1
        Registry.register(key, obj1)
        val listAfterSingle = Registry.lookup(key)
        assertEquals(1, listAfterSingle.size, "单个注册后 size = 1")
        assertEquals(obj1, listAfterSingle[0], "第一个元素应是 obj1")

        // 批量注册 obj2、obj3
        Registry.register(key, obj2, obj3)
        val listAfterBulk = Registry.lookup(key)
        // Bulk 注册不会检查重复，所以此时列表变成 [obj1, obj2, obj3]
        assertEquals(3, listAfterBulk.size, "混合注册后 size 应为 3")
        assertEquals(listOf(obj1, obj2, obj3), listAfterBulk, "列表顺序应为 [obj1, obj2, obj3]")

        // 再次单个注册 obj2（已经在列表里），单个注册会去重
        Registry.register(key, obj2)
        val listAfterSingleAgain = Registry.lookup(key)
        // obj2 已经存在，所以 size 仍为 3，不会重复添加
        assertEquals(3, listAfterSingleAgain.size, "单个注册重复对象应去重，size 不变")
        assertEquals(listAfterBulk, listAfterSingleAgain, "顺序与内容均不变")
    }

}