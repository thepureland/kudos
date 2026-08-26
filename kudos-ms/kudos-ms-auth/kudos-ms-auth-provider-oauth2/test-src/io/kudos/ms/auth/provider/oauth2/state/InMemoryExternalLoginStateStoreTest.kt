package io.kudos.ms.auth.provider.oauth2.state

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class InMemoryExternalLoginStateStoreTest {
    private val store = InMemoryExternalLoginStateStore()

    @Test
    fun stateCanBeConsumedOnlyOnce() {
        val state = ExternalLoginState("state-1", "tx-1", "provider-1", Instant.now().plusSeconds(60))

        assertTrue(store.create(state))
        assertFalse(store.create(state))
        assertNotNull(store.consume("state-1"))
        assertNull(store.consume("state-1"))
    }

    @Test
    fun expiredStateIsRejected() {
        assertTrue(
            store.create(ExternalLoginState("expired", "tx-1", "provider-1", Instant.now().minusSeconds(1)))
        )
        assertNull(store.consume("expired"))
    }
}
