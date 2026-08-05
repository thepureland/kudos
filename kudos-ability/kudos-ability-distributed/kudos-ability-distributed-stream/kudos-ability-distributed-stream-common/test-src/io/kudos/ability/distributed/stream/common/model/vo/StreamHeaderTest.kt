package io.kudos.ability.distributed.stream.common.model.vo

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.springframework.messaging.MessageHeaders
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [StreamHeader]:
 * - [StreamHeader.toContextParam] restoring tenant / datasource headers (and not mistakenly
 *   mapping userId into tenantId, the historical bug)
 * - [StreamHeader.initHeader] capturing destination and the current [KudosContext]
 *   (tenantId / userId / _datasourceTenantId), including the empty-context case
 * - the wire-format header key constants
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamHeaderTest {

    @AfterTest
    fun tearDown() {
        KudosContextHolder.clear()
    }

    @Test
    fun toContextParam_restoresTenantAndDatasourceHeaders() {
        val headers = MessageHeaders(
            mapOf(
                StreamHeader.TENANT_ID_KEY to "tenant-a",
                StreamHeader.DATA_SOURCE_ID_KEY to "ds-main",
                StreamHeader.DATASOURCE_TENANT_ID to "tenant-ds-a",
                StreamHeader.USER_ID_KEY to "user-should-not-be-tenant"
            )
        )

        val context = StreamHeader.toContextParam(headers)

        assertEquals("tenant-a", context.tenantId)
        assertEquals("ds-main", context.dataSourceId)
        assertEquals("tenant-ds-a", context._datasourceTenantId)
    }

    @Test
    fun initHeader_capturesDestinationAndCurrentContext() {
        KudosContextHolder.set(KudosContext().apply {
            tenantId = "tenant-1"
            _datasourceTenantId = "ds-tenant-1"
            user = object : IIdEntity<String> {
                override val id = "user-1"
            }
        })

        val header = StreamHeader.initHeader("topic-orders")

        assertEquals("topic-orders", header.destination)
        assertEquals("tenant-1", header.tenantId)
        assertEquals("ds-tenant-1", header._datasourceTenantId)
        assertEquals("user-1", header.userId)
        assertNull(header.username, "username is not propagated by initHeader")
        assertNull(header.dataSourceId, "dataSourceId is not propagated by initHeader")
    }

    @Test
    fun initHeader_handlesEmptyContextAndNullDestination() {
        KudosContextHolder.clear()

        val header = StreamHeader.initHeader(null)

        assertNull(header.destination)
        assertNull(header.tenantId)
        assertNull(header.userId)
        assertNull(header._datasourceTenantId)
    }

    @Test
    fun constants_keepWireCompatibleHeaderKeys() {
        assertEquals("destination", StreamHeader.TOPIC_KEY)
        assertEquals("tenantId", StreamHeader.TENANT_ID_KEY)
        assertEquals("dataSourceId", StreamHeader.DATA_SOURCE_ID_KEY)
        assertEquals("userId", StreamHeader.USER_ID_KEY)
        assertEquals("username", StreamHeader.USERNAME_KEY)
        assertEquals("_datasourceTenantId", StreamHeader.DATASOURCE_TENANT_ID)
        assertEquals("scst_produce_bind_name_", StreamHeader.SCST_BIND_NAME)
    }

}
