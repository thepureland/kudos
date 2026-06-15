package io.kudos.ability.web.guest.provider

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.web.guest.init.properties.GuestProperties
import org.mockito.Mockito
import org.springframework.data.redis.core.BoundValueOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit tests for [RedisGuestAccessStore] branches that the testcontainer-based
 * [RedisGuestAccessStoreTest] cannot exercise deterministically:
 *
 *  - `template()` routing: blank/null groupName → default template; non-blank groupName → the
 *    named template; unknown groupName → loud [IllegalStateException] naming the missing group;
 *  - `hasKey` returning `null` (a Redis pipeline/transaction quirk) must be treated as
 *    "key absent" — i.e. write + listener, never a blind `EXPIRE` on a key that may not exist;
 *  - a `null` listener (no [IGuestAccessListener] bean registered) must not NPE on the
 *    new-key path;
 *  - `getByHash` must return null when the stored value is not a Map (foreign data under the
 *    same key, e.g. another app sharing the prefix), instead of ClassCastException-ing.
 *
 * Redis collaborators are Mockito mocks; no I/O happens.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RedisGuestAccessStoreUnitTest {

    @Suppress("UNCHECKED_CAST")
    private fun mockTemplate(): RedisTemplate<Any, Any?> =
        Mockito.mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun mockValueOps(): ValueOperations<Any, Any?> =
        Mockito.mock(ValueOperations::class.java) as ValueOperations<Any, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun mockBoundValueOps(): BoundValueOperations<Any, Any?> =
        Mockito.mock(BoundValueOperations::class.java) as BoundValueOperations<Any, Any?>

    private fun properties(groupName: String? = null): GuestProperties = GuestProperties().apply {
        repository.groupName = groupName
        repository.prefix = "unit-prefix"
        repository.timeout = Duration.ofMinutes(7)
    }

    @Test
    fun store_existingKey_rollsTtlOnly_noListener() {
        val template = mockTemplate()
        Mockito.`when`(template.hasKey("unit-prefix:h1")).thenReturn(true)
        var fired = 0
        val store = RedisGuestAccessStore(
            properties(),
            RedisTemplates(mutableMapOf(), template),
        ) { fired++ }

        store.store(GuestAccess().apply { hash = "h1"; payload = mapOf("k" to "v") })

        Mockito.verify(template).expire("unit-prefix:h1", Duration.ofMinutes(7))
        Mockito.verify(template, Mockito.never()).opsForValue()
        assertEquals(0, fired, "TTL roll on a live key must not fire the active listener")
    }

    @Test
    fun store_newKey_writesWithTtl_andFiresListenerWithSameGuest() {
        val template = mockTemplate()
        val valueOps = mockValueOps()
        Mockito.`when`(template.hasKey("unit-prefix:h2")).thenReturn(false)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOps)
        var received: GuestAccess? = null
        val store = RedisGuestAccessStore(
            properties(),
            RedisTemplates(mutableMapOf(), template),
        ) { received = it }
        val guest = GuestAccess().apply { hash = "h2"; payload = mapOf("a" to "b") }

        store.store(guest)

        Mockito.verify(valueOps).set("unit-prefix:h2", mapOf("a" to "b"), Duration.ofMinutes(7))
        assertSame(guest, received, "listener must receive the exact stored GuestAccess instance")
    }

    @Test
    fun store_hasKeyNull_isTreatedAsAbsent() {
        // RedisTemplate.hasKey returns Boolean? — null (e.g. inside a pipeline) must route to
        // the "write" branch, not a blind EXPIRE on a possibly-missing key.
        val template = mockTemplate()
        val valueOps = mockValueOps()
        Mockito.`when`(template.hasKey("unit-prefix:h3")).thenReturn(null)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOps)
        val store = RedisGuestAccessStore(properties(), RedisTemplates(mutableMapOf(), template), null)

        store.store(GuestAccess().apply { hash = "h3" })

        Mockito.verify(valueOps).set("unit-prefix:h3", emptyMap<String, String>(), Duration.ofMinutes(7))
        Mockito.verify(template, Mockito.never()).expire(Mockito.any(), Mockito.any(Duration::class.java))
    }

    @Test
    fun store_newKey_nullListener_doesNotThrow() {
        val template = mockTemplate()
        val valueOps = mockValueOps()
        Mockito.`when`(template.hasKey("unit-prefix:h4")).thenReturn(false)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOps)
        val store = RedisGuestAccessStore(properties(), RedisTemplates(mutableMapOf(), template), null)

        store.store(GuestAccess().apply { hash = "h4"; payload = emptyMap() })

        Mockito.verify(valueOps).set("unit-prefix:h4", emptyMap<String, String>(), Duration.ofMinutes(7))
    }

    @Test
    fun store_missingHash_failsLoudWithGuidance() {
        val store = RedisGuestAccessStore(properties(), RedisTemplates(mutableMapOf(), mockTemplate()), null)

        val e = assertFailsWith<IllegalStateException> { store.store(GuestAccess()) }

        val message = assertNotNull(e.message)
        assertTrue(
            message.contains("hash") && message.contains("IGuestAccessService.hash"),
            "error must point the caller at the missing hash population step, was: $message",
        )
    }

    @Test
    fun template_blankGroupName_usesDefaultTemplate() {
        // Empty string (yml `group-name:` left blank binds to "") must behave like null.
        val defaultTemplate = mockTemplate()
        val namedTemplate = mockTemplate()
        Mockito.`when`(defaultTemplate.hasKey("unit-prefix:hb")).thenReturn(true)
        val store = RedisGuestAccessStore(
            properties(groupName = ""),
            RedisTemplates(mutableMapOf("data" to namedTemplate), defaultTemplate),
            null,
        )

        store.store(GuestAccess().apply { hash = "hb" })

        Mockito.verify(defaultTemplate).expire("unit-prefix:hb", Duration.ofMinutes(7))
        Mockito.verifyNoInteractions(namedTemplate)
    }

    @Test
    fun template_namedGroup_routesToThatTemplate_notDefault() {
        val defaultTemplate = mockTemplate()
        val namedTemplate = mockTemplate()
        Mockito.`when`(namedTemplate.hasKey("unit-prefix:hn")).thenReturn(true)
        val store = RedisGuestAccessStore(
            properties(groupName = "data"),
            RedisTemplates(mutableMapOf("data" to namedTemplate), defaultTemplate),
            null,
        )

        store.store(GuestAccess().apply { hash = "hn" })

        Mockito.verify(namedTemplate).expire("unit-prefix:hn", Duration.ofMinutes(7))
        Mockito.verifyNoInteractions(defaultTemplate)
    }

    @Test
    fun template_unknownGroup_failsLoudNamingTheGroup() {
        val store = RedisGuestAccessStore(
            properties(groupName = "ghost"),
            RedisTemplates(mutableMapOf(), mockTemplate()),
            null,
        )

        val e = assertFailsWith<IllegalStateException> {
            store.store(GuestAccess().apply { hash = "hx" })
        }

        val message = assertNotNull(e.message)
        assertTrue(message.contains("ghost"), "error must name the missing group, was: $message")
    }

    @Test
    fun getByHash_nonMapValueUnderKey_returnsNullInsteadOfClassCast() {
        val template = mockTemplate()
        val boundOps = mockBoundValueOps()
        Mockito.`when`(template.boundValueOps("unit-prefix:alien")).thenReturn(boundOps)
        Mockito.`when`(boundOps.get()).thenReturn("not-a-map")
        val store = RedisGuestAccessStore(properties(), RedisTemplates(mutableMapOf(), template), null)

        assertNull(store.getByHash("alien"))
    }

    @Test
    fun getByHash_mapValue_surfacesHashAndPayload() {
        val template = mockTemplate()
        val boundOps = mockBoundValueOps()
        Mockito.`when`(template.boundValueOps("unit-prefix:known")).thenReturn(boundOps)
        Mockito.`when`(boundOps.get()).thenReturn(mapOf("k" to "v"))
        val store = RedisGuestAccessStore(properties(), RedisTemplates(mutableMapOf(), template), null)

        val guest = assertNotNull(store.getByHash("known"))

        assertEquals("known", guest.hash)
        assertEquals(mapOf("k" to "v"), guest.payload)
    }
}
