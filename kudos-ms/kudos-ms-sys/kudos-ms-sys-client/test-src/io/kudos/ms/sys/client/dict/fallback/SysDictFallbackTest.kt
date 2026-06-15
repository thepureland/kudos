package io.kudos.ms.sys.client.dict.fallback

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [SysDictFallback].
 *
 * Coverage:
 *  - getActiveDictItems / getActiveDictItemMap return empty list / empty LinkedHashMap.
 *  - batchGetActiveDictItems / batchGetActiveDictItemMap return empty maps for empty,
 *    single and multiple pair lists.
 *  - Edge inputs: empty strings, Unicode dict types.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictFallbackTest {

    private val fallback = SysDictFallback()

    @Test
    fun getActiveDictItems_returnsEmptyList() {
        assertTrue(fallback.getActiveDictItems("sex", "kudos-sys").isEmpty())
        assertTrue(fallback.getActiveDictItems("", "").isEmpty())
        assertTrue(fallback.getActiveDictItems("字典-ünï", "服務").isEmpty())
    }

    @Test
    fun getActiveDictItemMap_returnsEmptyLinkedHashMap() {
        assertTrue(fallback.getActiveDictItemMap("sex", "kudos-sys").isEmpty())
        assertTrue(fallback.getActiveDictItemMap("", "").isEmpty())
    }

    @Test
    fun batchGetActiveDictItems_returnsEmptyMap() {
        assertTrue(fallback.batchGetActiveDictItems(emptyList()).isEmpty())
        assertTrue(fallback.batchGetActiveDictItems(listOf("sex" to "kudos-sys")).isEmpty())
        assertTrue(
            fallback.batchGetActiveDictItems(
                listOf("a" to "s1", "b" to "s2", "" to "")
            ).isEmpty()
        )
    }

    @Test
    fun batchGetActiveDictItemMap_returnsEmptyMap() {
        assertTrue(fallback.batchGetActiveDictItemMap(emptyList()).isEmpty())
        assertTrue(fallback.batchGetActiveDictItemMap(listOf("sex" to "kudos-sys")).isEmpty())
    }
}
