package io.kudos.ms.auth.core.credential.password

import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.auth.core.credential.password.listener.AuthPasswordHistoryLifecycleListener
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test

internal class AuthPasswordHistoryLifecycleListenerPureTest {

    private val dao = mock(AuthPasswordHistoryDao::class.java)
    private val listener = AuthPasswordHistoryLifecycleListener(dao)

    @Test
    fun singleDeletePurgesOwningUsersHistory() {
        listener.on(UserAccountDeleted("u1", "t1", "alice"))

        verify(dao).deleteByUserId("u1")
    }

    @Test
    fun batchDeletePurgesEveryOwningUserHistory() {
        listener.on(
            UserAccountBatchDeleted(
                listOf(
                    UserAccountBatchDeleted.Item("u1", "t1", "alice"),
                    UserAccountBatchDeleted.Item("u2", "t1", "bob"),
                )
            )
        )

        verify(dao).deleteByUserId("u1")
        verify(dao).deleteByUserId("u2")
    }
}
