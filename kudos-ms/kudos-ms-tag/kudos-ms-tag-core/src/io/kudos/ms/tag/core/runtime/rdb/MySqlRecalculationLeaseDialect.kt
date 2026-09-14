package io.kudos.ms.tag.core.runtime.rdb

import java.sql.Connection
import java.time.LocalDateTime

class MySqlRecalculationLeaseDialect : RecalculationLeaseDialect {
    override fun supports(productName: String) = productName.contains("MySQL", ignoreCase = true)
    override fun selectCandidateIds(connection: Connection, now: LocalDateTime, limit: Int) =
        select(connection, now, limit, " for update skip locked")
}
