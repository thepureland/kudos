package io.kudos.ms.auth.core.authentication.mfa.recovery

import io.kudos.ms.auth.core.authentication.mfa.recovery.dao.AuthRecoveryCodeDao
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.po.AuthRecoveryCode
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthRecoveryCodeDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthRecoveryCodeDao

    @Test
    fun codeIsSingleUseAndSetRevocationIsAccountScoped() {
        val suffix = UUID.randomUUID().toString()
        val tenantId = "t-${suffix.take(30)}"
        val userId = "u-${suffix.take(30)}"
        val setId = UUID.randomUUID().toString()
        val now = LocalDateTime.of(2026, 8, 25, 10, 0)
        dao.insert(code(tenantId, userId, setId, "a".repeat(64), now))
        dao.insert(code(tenantId, userId, setId, "b".repeat(64), now.plusSeconds(1)))

        assertEquals(setId, dao.findActiveSetId(tenantId, userId))
        assertEquals(2, dao.countUnused(tenantId, userId, setId))
        assertTrue(dao.consume(tenantId, userId, setId, "a".repeat(64), now.plusMinutes(1)))
        assertFalse(dao.consume(tenantId, userId, setId, "a".repeat(64), now.plusMinutes(1)))
        assertEquals(1, dao.countUnused(tenantId, userId, setId))
        assertEquals(2, dao.revokeActive(tenantId, userId, now.plusMinutes(2)))
        assertEquals(null, dao.findActiveSetId(tenantId, userId))
    }

    private fun code(
        tenantId: String,
        userId: String,
        setId: String,
        hash: String,
        createdAt: LocalDateTime,
    ): AuthRecoveryCode = AuthRecoveryCode {
        id = UUID.randomUUID().toString()
        this.tenantId = tenantId
        this.userId = userId
        this.setId = setId
        codeHash = hash
        this.createdAt = createdAt
        consumedAt = null
        revokedAt = null
    }
}
