package io.kudos.ms.tag.core

import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import java.util.UUID

internal abstract class TagDaoTestSupport {

    @BeforeEach
    fun prepareDatabase() {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:tag_dao_${UUID.randomUUID().toString().replace("-", "")};DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = "sa"
        }
        FlywayKit.migrate("tag", dataSource)
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect(dataSource, dialect = object : SqlDialect {})
        )
    }

    @AfterEach
    fun clearContext() {
        KudosContextHolder.clear()
    }
}
