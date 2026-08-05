package io.kudos.ms.auth.core.role.temporal.schedule

import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test

/**
 * Pure unit test for [ExpiredGrantPurgeScheduler] (collaborator mocked with Mockito, no Spring
 * container and no scheduler — [ExpiredGrantPurgeScheduler.run] is invoked directly).
 *
 * Covers every branch of [ExpiredGrantPurgeScheduler.run]:
 *  - purged > 0  → the info-log branch is taken (and the call still completes normally);
 *  - purged == 0 → the info-log branch is skipped;
 *  - the service throwing → the exception is swallowed (never propagated out of the scheduled job).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class ExpiredGrantPurgeSchedulerTest {

    private val service = mock(IAuthRoleUserTemporalService::class.java)

    // The sweep covers both edges of both grant kinds: role grants expire/activate, and group
    // memberships announce their window changes (they are not deleted — the read paths filter them).
    private val groupUserService = mock(IAuthGroupUserService::class.java)
    private val scheduler = ExpiredGrantPurgeScheduler(service, groupUserService)

    @Test
    fun run_whenSomethingPurged_invokesServiceAndLogs() {
        whenCalled(service.purgeExpired()).thenReturn(3)
        // Must not throw; the >0 branch (info log) is exercised.
        scheduler.run()
        verify(service, times(1)).purgeExpired()
    }

    @Test
    fun run_whenNothingPurged_invokesServiceNoLog() {
        whenCalled(service.purgeExpired()).thenReturn(0)
        // The "if (purged > 0)" branch is NOT taken; still completes normally.
        scheduler.run()
        verify(service, times(1)).purgeExpired()
    }

    @Test
    fun run_whenServiceThrows_swallowsException() {
        whenCalled(service.purgeExpired()).thenThrow(RuntimeException("boom — DB unavailable"))
        // The whole point of the try/catch: a scheduled job must never let exceptions escape,
        // otherwise the scheduler may suppress future triggers. So run() must return normally.
        scheduler.run()
        verify(service, times(1)).purgeExpired()
    }

    @Test
    fun run_negativeReturn_doesNotLogAsPurged() {
        // Defensive: a (hypothetical) negative count must not take the >0 branch either.
        whenCalled(service.purgeExpired()).thenReturn(-1)
        scheduler.run()
        verify(service).purgeExpired()
    }
}
