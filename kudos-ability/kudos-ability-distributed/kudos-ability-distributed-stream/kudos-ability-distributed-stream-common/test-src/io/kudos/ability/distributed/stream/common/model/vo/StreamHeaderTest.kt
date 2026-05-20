package io.kudos.ability.distributed.stream.common.model.vo

import org.springframework.messaging.MessageHeaders
import kotlin.test.Test
import kotlin.test.assertEquals


internal class StreamHeaderTest {

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

}
