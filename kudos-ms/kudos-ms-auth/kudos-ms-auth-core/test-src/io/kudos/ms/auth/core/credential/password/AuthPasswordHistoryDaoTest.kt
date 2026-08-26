package io.kudos.ms.auth.core.credential.password

import io.kudos.base.security.PasswordKit
import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.auth.core.credential.password.model.po.AuthPasswordHistory
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIfDockerInstalled
internal class AuthPasswordHistoryDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthPasswordHistoryDao

    @Test
    fun recentOrderingRetentionAndAccountIsolation() {
        val suffix = UUID.randomUUID().toString()
        val tenantId = "t-${suffix.take(30)}"
        val userId = "u-${suffix.take(30)}"
        val base = LocalDateTime.of(2026, 8, 24, 10, 0)
        repeat(4) { index ->
            dao.insert(history(tenantId, userId, "LOGIN", base.plusSeconds(index.toLong()), "password value $index"))
        }
        val otherUserId = "o-${suffix.take(30)}"
        dao.insert(history(tenantId, otherUserId, "LOGIN", base.plusMinutes(1), "isolated password"))

        assertEquals(3, dao.findRecent(tenantId, userId, "LOGIN", 3).size)
        assertEquals(1, dao.trimToSize(tenantId, userId, "LOGIN", 3))
        assertEquals(3, dao.findRecent(tenantId, userId, "LOGIN", 10).size)
        assertEquals(1, dao.findRecent(tenantId, otherUserId, "LOGIN", 10).size)
    }

    private fun history(
        tenantId: String,
        userId: String,
        purpose: String,
        recordedAt: LocalDateTime,
        password: String,
    ): AuthPasswordHistory = AuthPasswordHistory {
        id = UUID.randomUUID().toString()
        this.tenantId = tenantId
        this.userId = userId
        this.purpose = purpose
        passwordHash = PasswordKit.hash(password, 4)
        this.recordedAt = recordedAt
    }
}
