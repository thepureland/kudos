package io.kudos.ability.distributed.stream.common.model.po

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the [SysMqFailMsg] entity: factory creation via the companion [io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory]
 * and read/write of all mapped properties (id / topic / msgHeaderJson / msgBodyJson / createTime),
 * including empty-string and Unicode values.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMqFailMsgTest {

    @Test
    fun factory_createsMutableEntity() {
        val now = LocalDateTime.of(2026, 6, 11, 10, 30, 0)
        val entity = SysMqFailMsg().apply {
            id = "id-1"
            topic = "topic-訂單"
            msgHeaderJson = """{"h":"v"}"""
            msgBodyJson = ""
            createTime = now
        }

        assertEquals("id-1", entity.id)
        assertEquals("topic-訂單", entity.topic)
        assertEquals("""{"h":"v"}""", entity.msgHeaderJson)
        assertEquals("", entity.msgBodyJson)
        assertEquals(now, entity.createTime)
    }

}
