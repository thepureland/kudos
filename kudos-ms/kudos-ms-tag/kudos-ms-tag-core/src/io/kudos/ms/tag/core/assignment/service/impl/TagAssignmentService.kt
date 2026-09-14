package io.kudos.ms.tag.core.assignment.service.impl

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentResult
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentStatus
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentValidationException
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand
import io.kudos.ms.tag.core.assignment.model.TagResolutionCandidate
import io.kudos.ms.tag.core.assignment.service.iservice.ITagAssignmentService
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagManualAssignmentEventDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignmentEvent
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagManualAssignmentEvent
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagMembershipStore
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
open class TagAssignmentService(
    private val tagDao: TagDefinitionDao,
    private val tagSetDao: TagSetDao,
    private val subjectDao: TagSubjectDao,
    private val membershipStore: TagMembershipStore,
    private val assignmentIndex: TagAssignmentIndex,
    private val resolver: TagAssignmentResolver,
    private val assignmentEventDao: TagAssignmentEventDao,
    private val manualEventDao: TagManualAssignmentEventDao,
    private val transactionExecutor: TagAssignmentTransactionExecutor,
) : ITagAssignmentService {

    override fun assignManual(command: AssignManualTagCommand): ManualAssignmentResult {
        val checksum = checksum(
            command.eventId, command.requestId, command.subjectKey, command.tagId, "ASSIGN", null,
            command.operatorId, command.operatorName, command.reason, command.effectiveUntil, command.occurredAt,
        )
        replay(command.eventId, checksum)?.let { return it }
        val tag = validate(command.eventId, command.subjectKey, command.tagId, command.operatorId, command.effectiveUntil, command.occurredAt)
        if (!tag.manualAssignable) fail(TagErrorCode.MANUAL_ASSIGNMENT_NOT_ALLOWED, "Tag [${tag.code}] does not allow manual assignment.")
        return transactionExecutor.execute {
            ensureAndLockSubject(command.subjectKey)
            val audit = audit(
                command.eventId, checksum, command.requestId, command.subjectKey, command.tagId, "ASSIGN", null,
                command.operatorId, command.operatorName, command.reason, command.effectiveUntil, command.occurredAt,
            )
            manualEventDao.insert(audit)
            membershipStore.replace(
                command.subjectKey,
                command.tagId,
                TagMembershipSource.MANUAL,
                command.eventId,
                true,
                membershipStore.nextVersion(command.subjectKey),
                effectiveUntil = command.effectiveUntil,
            )
            val delta = reconcile(command.subjectKey, tag.tagSetId, "MANUAL", command.eventId)
            audit.processStatus = "APPLIED"
            check(manualEventDao.update(audit))
            ManualAssignmentResult(command.eventId, ManualAssignmentStatus.APPLIED, delta)
        }
    }

    override fun removeManual(command: RemoveManualTagCommand): ManualAssignmentResult {
        val checksum = checksum(
            command.eventId, command.requestId, command.subjectKey, command.tagId, "REMOVE", command.manualSourceRef,
            command.operatorId, command.operatorName, command.reason, null, command.occurredAt,
        )
        replay(command.eventId, checksum)?.let { return it }
        val tag = validate(command.eventId, command.subjectKey, command.tagId, command.operatorId, null, command.occurredAt)
        membershipStore.find(command.subjectKey, command.tagId, TagMembershipSource.MANUAL, command.manualSourceRef)
            ?: fail(TagErrorCode.INVALID_MANUAL_ASSIGNMENT, "Manual membership [${command.manualSourceRef}] does not exist.")
        return transactionExecutor.execute {
            ensureAndLockSubject(command.subjectKey)
            val audit = audit(
                command.eventId, checksum, command.requestId, command.subjectKey, command.tagId, "REMOVE", command.manualSourceRef,
                command.operatorId, command.operatorName, command.reason, null, command.occurredAt,
            )
            manualEventDao.insert(audit)
            membershipStore.replace(
                command.subjectKey,
                command.tagId,
                TagMembershipSource.MANUAL,
                command.manualSourceRef,
                false,
                membershipStore.nextVersion(command.subjectKey),
            )
            val delta = reconcile(command.subjectKey, tag.tagSetId, "MANUAL", command.eventId)
            audit.processStatus = "APPLIED"
            check(manualEventDao.update(audit))
            ManualAssignmentResult(command.eventId, ManualAssignmentStatus.APPLIED, delta)
        }
    }

    private fun reconcile(key: TagSubjectKey, tagSetId: String?, causeType: String, causeRef: String): AssignmentDelta {
        val tagSet = tagSetId?.let { requireNotNull(tagSetDao.get(it)) }
        tagSet?.defaultTagId?.let { defaultTagId ->
            val sourceRef = "set:${tagSet.id}"
            val existing = membershipStore.find(key, defaultTagId, TagMembershipSource.DEFAULT, sourceRef)
            if (existing == null || existing.effectiveUntil?.isBefore(Instant.now()) == true) {
                membershipStore.replace(
                    key, defaultTagId, TagMembershipSource.DEFAULT, sourceRef, true, membershipStore.nextVersion(key),
                )
            }
        }
        val candidates = membershipStore.listActive(key).mapNotNull { membership ->
            val tag = tagDao.get(membership.tagId)?.takeIf { it.active } ?: return@mapNotNull null
            if (tag.tagSetId != tagSetId) return@mapNotNull null
            TagResolutionCandidate(
                membership,
                tag.tagSetId,
                tagSet?.cardinality ?: TagAttributeCardinality.MULTIPLE,
                tag.setPriority,
            )
        }
        val delta = assignmentIndex.replaceForSet(key, tagSetId, resolver.resolve(candidates))
        emitDeltaEvents(key, delta, causeType, causeRef)
        return delta
    }

    private fun emitDeltaEvents(key: TagSubjectKey, delta: AssignmentDelta, causeType: String, causeRef: String) {
        delta.assignedTagIds.forEach { insertAssignmentEvent(key, it, "ASSIGNED", causeType, causeRef, delta.assignmentVersion) }
        delta.removedTagIds.forEach { insertAssignmentEvent(key, it, "REMOVED", causeType, causeRef, delta.assignmentVersion) }
    }

    private fun insertAssignmentEvent(
        key: TagSubjectKey,
        tagId: String,
        operation: String,
        causeType: String,
        causeRef: String,
        version: Long,
    ) {
        assignmentEventDao.insert(TagAssignmentEvent().apply {
            eventId = UUID.randomUUID().toString()
            tenantId = key.tenantId
            subjectType = key.subjectType
            subjectId = key.subjectId
            this.tagId = tagId
            this.operation = operation
            this.causeType = causeType
            this.causeRef = causeRef
            assignmentVersion = version
            occurredTime = LocalDateTime.now(ZoneOffset.UTC)
        })
    }

    private fun validate(
        eventId: String,
        key: TagSubjectKey,
        tagId: String,
        operatorId: String,
        effectiveUntil: Instant?,
        occurredAt: Instant,
    ): TagDefinition {
        if (eventId.isBlank() || eventId.length > 36) fail(TagErrorCode.INVALID_MANUAL_ASSIGNMENT, "Event ID must contain 1 to 36 characters.")
        if (operatorId.isBlank()) fail(TagErrorCode.INVALID_MANUAL_ASSIGNMENT, "Operator ID must not be blank.")
        if (effectiveUntil != null && effectiveUntil <= occurredAt) {
            fail(TagErrorCode.INVALID_MANUAL_ASSIGNMENT, "Manual assignment expiry must be later than its occurrence time.")
        }
        return tagDao.get(tagId)
            ?.takeIf { it.active && it.tenantId == key.tenantId && it.subjectType == key.subjectType }
            ?: fail(TagErrorCode.INVALID_TAG_CODE, "Tag [$tagId] does not exist in the subject scope.")
    }

    private fun ensureAndLockSubject(key: TagSubjectKey) {
        if (subjectDao.find(key) == null) {
            val now = LocalDateTime.now(ZoneOffset.UTC)
            check(subjectDao.insertSubject(TagSubject().apply {
                subjectId = key.subjectId
                tenantId = key.tenantId
                subjectType = key.subjectType
                displayName = null
                profileJson = null
                stateVersion = 0
                firstSeenTime = now
                updateTime = now
            }))
        }
        check(subjectDao.lockAssignments(key, LocalDateTime.now(ZoneOffset.UTC)))
    }

    private fun replay(eventId: String, checksum: String): ManualAssignmentResult? = manualEventDao.get(eventId)?.let { existing ->
        if (existing.payloadChecksum != checksum) {
            fail(TagErrorCode.IDEMPOTENCY_CONFLICT, "Manual assignment event [$eventId] has a different payload.")
        }
        ManualAssignmentResult(eventId, ManualAssignmentStatus.DUPLICATE, AssignmentDelta(emptySet(), emptySet(), 0))
    }

    private fun audit(
        eventId: String,
        checksum: String,
        requestId: String?,
        key: TagSubjectKey,
        tagId: String,
        operation: String,
        sourceRef: String?,
        operatorId: String,
        operatorName: String?,
        reason: String?,
        effectiveUntil: Instant?,
        occurredAt: Instant,
    ) = TagManualAssignmentEvent().apply {
        this.eventId = eventId
        payloadChecksum = checksum
        this.requestId = requestId
        tenantId = key.tenantId
        subjectType = key.subjectType
        subjectId = key.subjectId
        this.tagId = tagId
        this.operation = operation
        manualSourceRef = sourceRef
        this.operatorId = operatorId
        this.operatorName = operatorName
        this.reason = reason
        this.effectiveUntil = effectiveUntil?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
        occurredTime = occurredAt.atOffset(ZoneOffset.UTC).toLocalDateTime()
        receivedTime = LocalDateTime.now(ZoneOffset.UTC)
        processStatus = "RECEIVED"
    }

    private fun checksum(vararg values: Any?): String {
        val canonical = values.joinToString("|") { value -> value?.toString()?.let { "${it.length}:$it" } ?: "-" }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun fail(code: TagErrorCode, message: String): Nothing =
        throw ManualAssignmentValidationException(code, message)
}
