package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


/**
 * Collects what shadow mode observed, so the migration work-list is something an operator can *read*
 * rather than something they have to grep for.
 *
 * The WARN log is still emitted — logs are the durable record — but a log line per query is not a
 * work-list: it has no counts, no first/last seen, and no way to tell "this is one endpoint I forgot"
 * from "this is a hot path running a thousand times a minute". Both facts change what you do next.
 *
 * Deliberately **in-memory and bounded**. This is diagnostic scaffolding for a migration, not an
 * audit trail; persisting it would mean a table, a retention policy and a cleanup job for something
 * that is deleted the moment shadow mode is switched off.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RowScopeShadowRecorder(
    /** Distinct findings retained. Beyond this, new *kinds* of finding are dropped and counted. */
    private val capacity: Int = 500,
) {

    private val log = LogFactory.getLog(this::class)

    private val findings = ConcurrentHashMap<String, Finding>()
    private val dropped = AtomicLong(0)

    /** What shadow mode saw. */
    enum class Kind {
        /** A predicate would have been appended — verify it says what you meant. */
        WOULD_FILTER,

        /**
         * A scoped entity was queried with no subject and no declared system authority. The
         * actionable one: this call needs `DataScopeContext.runAsSystem { }` or `@SystemScoped`.
         */
        WOULD_FAIL,
    }

    /** One distinct finding, with how often and how recently it happened. */
    data class Finding(
        val kind: Kind,
        val entity: String,
        val table: String,
        val detail: String,
        val count: Long,
        val firstSeen: LocalDateTime,
        val lastSeen: LocalDateTime,
    )

    /**
     * Records one observation, merging into an existing finding when it is the same kind on the same
     * entity — otherwise a hot path would bury everything else under thousands of identical rows.
     */
    open fun record(kind: Kind, entity: String, table: String, detail: String) {
        val key = "${kind}|${entity}|${detail}"
        val now = LocalDateTime.now()
        val existing = findings[key]
        if (existing != null) {
            findings[key] = existing.copy(count = existing.count + 1, lastSeen = now)
            return
        }
        if (findings.size >= capacity) {
            val total = dropped.incrementAndGet()
            // Say so, loudly and once in a while: a silently truncated work-list reads as
            // "nothing left to do", which is the opposite of the truth.
            if (total == 1L || total % 100 == 0L) {
                log.warn("[row-scope][shadow] finding buffer full (${capacity}); ${total} distinct finding(s) dropped.")
            }
            return
        }
        findings[key] = Finding(kind, entity, table, detail, 1, now, now)
    }

    /** Everything observed so far, most recent first. */
    open fun findings(): List<Finding> = findings.values.sortedByDescending { it.lastSeen }

    /** How many distinct findings were dropped because the buffer was full; 0 when the list is complete. */
    open fun droppedCount(): Long = dropped.get()

    /** Clears the list — for after you have acted on it, so the next run starts from a clean slate. */
    open fun clear() {
        findings.clear()
        dropped.set(0)
    }
}
