package io.kudos.ms.tag.core.runtime.rdb

import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime

interface RecalculationLeaseDialect {
    fun supports(productName: String): Boolean
    fun selectCandidateIds(connection: Connection, now: LocalDateTime, limit: Int): List<String>

    fun select(connection: Connection, now: LocalDateTime, limit: Int, lockingClause: String): List<String> {
        val sql = """select id from tag_recalculation_job
            where attempt_count < max_attempts and (
                (status in ('PENDING', 'RETRY_WAIT') and available_time <= ?)
                or (status = 'RUNNING' and lease_until <= ?)
            )
            order by priority desc, available_time asc, id asc
            limit ?$lockingClause""".trimIndent()
        return connection.prepareStatement(sql).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setTimestamp(2, Timestamp.valueOf(now))
            statement.setInt(3, limit)
            statement.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.getString(1)) } }
        }
    }
}
