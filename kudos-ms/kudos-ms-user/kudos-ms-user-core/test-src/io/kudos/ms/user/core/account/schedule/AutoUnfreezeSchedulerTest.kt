package io.kudos.ms.user.core.account.schedule

import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test

/**
 * Pure unit test for [AutoUnfreezeScheduler].
 *
 * The scheduled job only delegates to [IUserAccountService.cleanExpiredFreezes] and must never let an
 * exception escape (otherwise the scheduler would mark the trigger as failed). All three branches are
 * exercised here with a Mockito-mocked service, no Spring context / DB required:
 *  - cleared > 0  (logs at info level)
 *  - cleared == 0 (no info log)
 *  - service throws (swallowed by the try/catch)
 *
 * @author K
 * @since 1.0.0
 */
internal class AutoUnfreezeSchedulerTest {

    @Test
    fun run_withClearedRecords_invokesServiceOnce() {
        val service = mock(IUserAccountService::class.java)
        whenCalled(service.cleanExpiredFreezes()).thenReturn(3)
        val scheduler = AutoUnfreezeScheduler(service)

        scheduler.run()

        verify(service, times(1)).cleanExpiredFreezes()
    }

    @Test
    fun run_withZeroCleared_invokesServiceOnce() {
        val service = mock(IUserAccountService::class.java)
        whenCalled(service.cleanExpiredFreezes()).thenReturn(0)
        val scheduler = AutoUnfreezeScheduler(service)

        scheduler.run()

        verify(service, times(1)).cleanExpiredFreezes()
    }

    @Test
    fun run_serviceThrows_exceptionIsSwallowed() {
        val service = mock(IUserAccountService::class.java)
        whenCalled(service.cleanExpiredFreezes()).thenThrow(RuntimeException("boom"))
        val scheduler = AutoUnfreezeScheduler(service)

        // Must not propagate; the scheduled job swallows it. Returning normally is the assertion.
        scheduler.run()

        verify(service, times(1)).cleanExpiredFreezes()
    }
}
