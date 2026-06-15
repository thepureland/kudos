package io.kudos.ability.cache.common.init.properties

import io.kudos.ability.cache.common.support.CacheConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the [CacheItemsProperties] container.
 *
 * Coverage:
 *  - both lists default to empty mutable lists.
 *  - both lists are reassignable and mutable.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheItemsPropertiesTest {

    @Test
    fun defaults_areEmptyMutableLists() {
        val props = CacheItemsProperties()
        assertTrue(props.cacheItems.isEmpty())
        assertTrue(props.cacheItemConfigs.isEmpty())
        // mutable: adding does not throw
        props.cacheItems.add("name=A&strategy=REMOTE")
        props.cacheItemConfigs.add(CacheConfig().apply { name = "B" })
        assertEquals(1, props.cacheItems.size)
        assertEquals(1, props.cacheItemConfigs.size)
    }

    @Test
    fun lists_areReassignable() {
        val props = CacheItemsProperties().apply {
            cacheItems = mutableListOf("name=X&strategy=LOCAL_REMOTE")
            cacheItemConfigs = mutableListOf(CacheConfig().apply { name = "Y"; strategy = "REMOTE" })
        }
        assertEquals(listOf("name=X&strategy=LOCAL_REMOTE"), props.cacheItems)
        assertEquals("Y", props.cacheItemConfigs.single().name)
    }
}
