package io.kudos.ms.tag.core.query

import com.mysql.cj.jdbc.MysqlDataSource
import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.query.service.impl.TagQueryService
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AllTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AnyTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.NotTags
import io.kudos.ms.tag.core.runtime.port.TagKeysetPage
import io.kudos.ms.tag.core.runtime.rdb.TagAssignmentQueryCompiler
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MySqlTestContainer
import io.kudos.test.container.containers.PostgresTestContainer
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.postgresql.ds.PGSimpleDataSource
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TagQueryDialectContractTest {

    @AfterEach
    fun clearContext() {
        KudosContextHolder.clear()
    }

    @Test
    fun compilesPortableParameterizedSqlWithIsolationAndExpiryOnEveryLeaf() {
        val compiled = TagAssignmentQueryCompiler().compile(
            tenantId = "tenant-a",
            subjectType = "estate.house",
            expression = AllTags(
                setOf("three-bedroom", "area-100-plus"),
                listOf(NotTags(AnyTags(setOf("suspended", "hidden")))),
            ),
            tagIdsByCode = mapOf(
                "three-bedroom" to "tag-1",
                "area-100-plus" to "tag-2",
                "suspended" to "tag-3",
                "hidden" to "tag-4",
            ),
            page = TagKeysetPage(afterSubjectId = "house-10", size = 25),
            now = Instant.parse("2026-09-14T00:00:00Z"),
        )

        assertTrue(compiled.sql.startsWith("select s.subject_id from tag_subject s"))
        assertTrue(compiled.sql.contains("order by s.subject_id asc limit ?"))
        assertEquals(3, Regex("a\\d+\\.tenant_id = \\?").findAll(compiled.sql).count())
        assertEquals(3, Regex("a\\d+\\.subject_type = \\?").findAll(compiled.sql).count())
        assertEquals(3, Regex("effective_until is null or a\\d+\\.effective_until > \\?").findAll(compiled.sql).count())
        assertFalse(compiled.sql.contains("tag_attribute_state"))
        assertFalse(compiled.sql.contains('`'))
        assertEquals("tenant-a", compiled.parameters.first())
        assertEquals(26, compiled.parameters.last())
    }

    @Test
    fun h2ExecutesGroupedAllTagsQuery() {
        verifyGroupedQuery(JdbcDataSource().apply {
            setURL("jdbc:h2:mem:tag_query_${suffix()};DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = "sa"
        })
    }

    @Test
    @EnabledIfDockerInstalled
    fun mysqlExecutesGroupedAllTagsQuery() {
        val container = MySqlTestContainer.startIfNeeded(null)
        val host = container.ports.first().ip
        val port = requireNotNull(container.ports.first().publicPort)
        val databaseName = "tag_query_${suffix()}"
        val adminUrl = "jdbc:mysql://$host:$port/?useSSL=false&allowPublicKeyRetrieval=true"
        DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
            it.createStatement().use { statement -> statement.executeUpdate("create database $databaseName") }
        }
        try {
            verifyGroupedQuery(MysqlDataSource().apply {
                setURL("jdbc:mysql://$host:$port/$databaseName?useSSL=false&allowPublicKeyRetrieval=true")
                user = MySqlTestContainer.USERNAME
                password = MySqlTestContainer.PASSWORD
            })
        } finally {
            KudosContextHolder.clear()
            DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
                it.createStatement().use { statement -> statement.executeUpdate("drop database $databaseName") }
            }
        }
    }

    @Test
    @EnabledIfDockerInstalled
    fun postgresqlExecutesGroupedAllTagsQuery() {
        val databaseName = "tag_query_${suffix()}"
        val container = PostgresTestContainer.startIfNeeded(null, databaseName)
        verifyGroupedQuery(PGSimpleDataSource().apply {
            serverNames = arrayOf(container.ports.first().ip)
            portNumbers = intArrayOf(requireNotNull(container.ports.first().publicPort))
            this.databaseName = databaseName
            user = PostgresTestContainer.USERNAME
            password = PostgresTestContainer.PASSWORD
        })
    }

    private fun verifyGroupedQuery(dataSource: DataSource) {
        FlywayKit.migrate("tag", dataSource)
        seed(dataSource)
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect(dataSource, dialect = object : SqlDialect {})
        )
        val tagDao = TagDefinitionDao()
        val index = RdbTagAssignmentIndex(TagAssignmentDao(), TagAssignmentEventDao(), tagDao, TagSetDao())
        val result = TagQueryService(tagDao, index).findSubjects(
            TagQueryRequest(TENANT, SUBJECT_TYPE, AllTags(TAG_CODES.toSet()))
        )

        assertEquals(listOf("subject-complete"), result.subjectIds)
    }

    private fun seed(dataSource: DataSource) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """insert into tag_subject_type
                       (id, code, name, owner_service_code, active, built_in, version, create_time, update_time)
                       values ('00000000-0000-0000-0000-000000000001', '$SUBJECT_TYPE', 'House', 'estate', true, false, 0, current_timestamp, current_timestamp)""".trimIndent()
                )
                TAG_CODES.forEachIndexed { index, code ->
                    statement.executeUpdate(
                        """insert into tag_definition
                           (id, tenant_id, subject_type, code, name, set_priority, manual_assignable, active, built_in, version, create_time, update_time)
                           values ('${tagId(index)}', '$TENANT', '$SUBJECT_TYPE', '$code', '$code', 0, true, true, false, 0, current_timestamp, current_timestamp)""".trimIndent()
                    )
                }
                listOf("subject-complete", "subject-expired").forEach { subjectId ->
                    statement.executeUpdate(
                        """insert into tag_subject
                           (tenant_id, subject_type, subject_id, state_version, first_seen_time, update_time)
                           values ('$TENANT', '$SUBJECT_TYPE', '$subjectId', 0, current_timestamp, current_timestamp)""".trimIndent()
                    )
                }
                TAG_CODES.forEachIndexed { index, _ ->
                    statement.executeUpdate(assignmentSql("subject-complete", index, "null"))
                    val expiry = if (index == TAG_CODES.lastIndex) "timestamp '2000-01-01 00:00:00'" else "null"
                    statement.executeUpdate(assignmentSql("subject-expired", index, expiry))
                }
            }
        }
    }

    private fun assignmentSql(subjectId: String, tagIndex: Int, expiry: String) =
        """insert into tag_assignment
           (tenant_id, subject_type, subject_id, tag_id, assignment_version, materialized_time, effective_until, update_time)
           values ('$TENANT', '$SUBJECT_TYPE', '$subjectId', '${tagId(tagIndex)}', 1, current_timestamp, $expiry, current_timestamp)""".trimIndent()

    private fun tagId(index: Int) = "00000000-0000-0000-0000-${(index + 10).toString().padStart(12, '0')}"

    private fun suffix() = UUID.randomUUID().toString().replace("-", "")

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "estate.house"
        val TAG_CODES = listOf("tag-one", "tag-two", "tag-three", "tag-four")
    }
}
