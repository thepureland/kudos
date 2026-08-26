package io.kudos.ms.auth.core.authentication

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.core.authentication.store.IAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.store.RedisAuthenticationTransactionStore
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real Redis verification for cross-instance create/CAS and serialization. */
@EnabledIfDockerInstalled
internal class RedisAuthenticationTransactionStoreTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Resource
    private lateinit var configuredStore: IAuthenticationTransactionStore

    @Test
    fun twoInstancesShareAtomicCreateAndVersionedUpdates() {
        assertIs<RedisAuthenticationTransactionStore>(configuredStore)
        val first = RedisAuthenticationTransactionStore(redisTemplates)
        val second = RedisAuthenticationTransactionStore(redisTemplates)
        val now = Instant.now()
        val transaction = AuthenticationTransaction(
            id = UUID.randomUUID().toString(),
            tenantId = "t-1",
            purpose = AuthenticationTransactionPurposeEnum.STEP_UP,
            initiatorUserId = "u-1",
            sourceSessionId = "session-1",
            requiredAcr = "urn:kudos:acr:mfa",
            userId = "u-1",
            status = AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
            nextActions = setOf(AuthenticationActionEnum.SELECT_METHOD),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(60),
        )

        assertTrue(first.create(transaction))
        assertFalse(second.create(transaction))
        assertEquals(transaction, second.get(transaction.id))

        val updated = assertNotNull(first.save(transaction.copy(errorCode = "first"), 0))
        assertEquals(1, updated.version)
        assertNull(second.save(transaction.copy(errorCode = "stale"), 0))
        assertEquals("first", second.get(transaction.id)?.errorCode)
    }
}
