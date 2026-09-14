package io.kudos.ms.tag.core.runtime.rdb

import java.sql.Connection
import java.time.LocalDateTime

class H2RecalculationLeaseDialect : RecalculationLeaseDialect {
    override fun supports(productName: String) = productName.contains("H2", ignoreCase = true)
    override fun selectCandidateIds(connection: Connection, now: LocalDateTime, limit: Int) = select(connection, now, limit, "")
}
