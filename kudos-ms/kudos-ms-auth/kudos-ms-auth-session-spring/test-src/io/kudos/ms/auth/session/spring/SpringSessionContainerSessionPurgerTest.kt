package io.kudos.ms.auth.session.spring

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.MapSession
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SpringSessionContainerSessionPurgerTest {

    @Test
    fun theCopiedIndexAttributeNameStillMatchesSpringSession() {
        // The issuing module sets this attribute without depending on Spring Session. If the constant ever
        // changed upstream, the copy would keep compiling and silently stop being indexed — and the purger
        // would then find nothing while looking like it worked.
        assertEquals(
            FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
            AuthenticationSession.PRINCIPAL_INDEX_SESSION_ATTRIBUTE,
        )
    }

    @Test
    fun onlyTheRevokedSessionsOfThatUserAreDeleted() {
        val repository = RecordingSessionRepository(
            "container-1" to "logical-1",
            "container-2" to "logical-2",
            "container-3" to "logical-3",
        )

        SpringSessionContainerSessionPurger(repository).purge("t-1", "u-1", setOf("logical-1", "logical-3"))

        // The principal index answers "every session of this user", which is a superset when one session was
        // revoked; the untouched one must survive.
        assertEquals(listOf("container-1", "container-3"), repository.deleted)
    }

    @Test
    fun aContainerSessionWithoutOurAttributeIsLeftAlone() {
        // Sessions created by something other than the Kudos login flow are not ours to delete.
        val repository = RecordingSessionRepository("container-1" to null, "container-2" to "logical-2")

        SpringSessionContainerSessionPurger(repository).purge("t-1", "u-1", setOf("logical-2"))

        assertEquals(listOf("container-2"), repository.deleted)
    }

    @Test
    fun nothingIsAskedOfTheStoreWhenThereIsNothingToPurge() {
        val repository = RecordingSessionRepository("container-1" to "logical-1")

        SpringSessionContainerSessionPurger(repository).purge("t-1", "u-1", emptySet())

        assertEquals(0, repository.lookups)
        assertEquals(emptyList(), repository.deleted)
    }

    @Test
    fun aUserWithNoContainerSessionsIsNotAnError() {
        val repository = RecordingSessionRepository()

        SpringSessionContainerSessionPurger(repository).purge("t-1", "u-1", setOf("logical-1"))

        assertEquals(emptyList(), repository.deleted)
    }

    private class RecordingSessionRepository(
        vararg sessions: Pair<String, String?>,
    ) : FindByIndexNameSessionRepository<MapSession> {
        private val stored: MutableMap<String, MapSession> = sessions.associate { (id, logicalId) ->
            id to MapSession(id).apply {
                logicalId?.let { setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, it) }
            }
        }.toMutableMap()

        val deleted = mutableListOf<String>()
        var lookups = 0

        override fun findByIndexNameAndIndexValue(indexName: String, indexValue: String): Map<String, MapSession> {
            lookups++
            return stored.toMap()
        }

        override fun createSession(): MapSession = MapSession()

        override fun save(session: MapSession) {
            stored[session.id] = session
        }

        override fun findById(id: String): MapSession? = stored[id]

        override fun deleteById(id: String) {
            deleted += id
            stored.remove(id)
        }
    }

}
