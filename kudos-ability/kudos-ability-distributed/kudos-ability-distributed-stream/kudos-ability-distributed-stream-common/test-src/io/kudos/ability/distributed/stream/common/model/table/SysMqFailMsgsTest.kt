package io.kudos.ability.distributed.stream.common.model.table

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the [SysMqFailMsgs] table binding object: table name and column names matching the
 * `sys_mq_fail_msg` DDL, and each column carrying an entity binding.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMqFailMsgsTest {

    @Test
    fun table_mapsDocumentedTableAndColumnNames() {
        assertEquals("sys_mq_fail_msg", SysMqFailMsgs.tableName)
        assertEquals("topic", SysMqFailMsgs.topic.name)
        assertEquals("msg_header_json", SysMqFailMsgs.msgHeaderJson.name)
        assertEquals("msg_body_json", SysMqFailMsgs.msgBodyJson.name)
        assertEquals("create_time", SysMqFailMsgs.createTime.name)
    }

    @Test
    fun columns_areBoundToEntityProperties() {
        assertNotNull(SysMqFailMsgs.topic.binding)
        assertNotNull(SysMqFailMsgs.msgHeaderJson.binding)
        assertNotNull(SysMqFailMsgs.msgBodyJson.binding)
        assertNotNull(SysMqFailMsgs.createTime.binding)
    }

}
