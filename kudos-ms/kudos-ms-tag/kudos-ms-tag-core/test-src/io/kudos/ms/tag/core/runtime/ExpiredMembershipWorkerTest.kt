package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagSetCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentResolver
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentService
import io.kudos.ms.tag.core.assignment.service.impl.TagAssignmentTransactionExecutor
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagManualAssignmentEventDao
import io.kudos.ms.tag.core.runtime.job.ExpiredMembershipWorker
import io.kudos.ms.tag.core.runtime.job.TagRecalculationProperties
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.rdb.RdbTagMembershipStore
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class ExpiredMembershipWorkerTest : TagDaoTestSupport() {
    private val tagDao = TagDefinitionDao()
    private val tagSetDao = TagSetDao()
    private val membershipDao = TagMembershipDao()
    private val assignmentDao = TagAssignmentDao()
    private val assignmentEventDao = TagAssignmentEventDao()
    private val membershipStore = RdbTagMembershipStore(membershipDao)
    private val service = TagAssignmentService(
        tagDao,
        tagSetDao,
        TagSubjectDao(),
        membershipDao,
        membershipStore,
        RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao),
        TagAssignmentResolver(),
        assignmentEventDao,
        TagManualAssignmentEventDao(),
        TagAssignmentTransactionExecutor(),
    )

    @Test
    fun `expired membership is deactivated assignments re-resolved and events emitted once`() {
        val catalog = TagCatalogService(TagSubjectTypeDao(), TagAttributeDefinitionDao(), tagSetDao, tagDao)
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand("estate.house", "House", "estate"))
        val set = catalog.createTagSet(
            CreateTagSetCommand("tenant-a", "estate.house", "quality", "Quality", TagAttributeCardinality.SINGLE)
        )
        val fallback = catalog.createTag(CreateTagCommand("tenant-a", "estate.house", "ordinary", "Ordinary", set.id))
        val premium = catalog.createTag(
            CreateTagCommand("tenant-a", "estate.house", "premium", "Premium", set.id, setPriority = 10)
        )
        catalog.setDefaultTag("tenant-a", set.id, fallback.id)
        val key = TagSubjectKey("tenant-a", "estate.house", "house-1")
        val eventId = UUID.randomUUID().toString()
        val expiry = Instant.now().plusSeconds(3_600)
        service.assignManual(
            AssignManualTagCommand(
                eventId, null, key, premium.id, "operator", null, null, expiry,
                expiry.minusSeconds(3_600),
            )
        )
        val membership = requireNotNull(
            membershipDao.find(key, premium.id, TagMembershipSource.MANUAL, eventId)
        )
        // Assignment was materialized before the deterministic worker clock reaches the expiry.
        assertEquals(listOf(premium.id), assignmentDao.list(key).map { it.tagId })

        val worker = ExpiredMembershipWorker(
            membershipDao,
            service,
            TagRecalculationProperties(expiryBatchSize = 10),
            Clock.fixed(expiry.plusSeconds(1), ZoneOffset.UTC),
        )
        assertEquals(1, worker.runOnce())
        assertEquals(0, worker.runOnce())

        assertFalse(requireNotNull(membershipDao.get(membership.id)).active)
        assertEquals(listOf(fallback.id), assignmentDao.list(key).map { it.tagId })
        val expiryEvents = assignmentEventDao.list(key).filter { it.causeRef == "expiry:${membership.id}" }
        assertEquals(setOf("ASSIGNED", "REMOVED"), expiryEvents.map { it.operation }.toSet())
        assertEquals(2, expiryEvents.size)
    }
}
