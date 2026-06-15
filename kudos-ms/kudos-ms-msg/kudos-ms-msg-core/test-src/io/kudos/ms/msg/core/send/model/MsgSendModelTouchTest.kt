package io.kudos.ms.msg.core.send.model

import io.kudos.ms.msg.core.instance.dao.MsgInstanceDao
import io.kudos.ms.msg.core.instance.model.table.MsgInstances
import io.kudos.ms.msg.core.send.dao.MsgSendDao
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.model.table.MsgSends
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Loads the ktorm table-binding singletons and the DAO classes for the send/instance subtrees, so
 * their column bindings and no-arg constructors are exercised without a database. Pure metadata
 * access only — no query is executed.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendModelTouchTest {

    @Test
    fun msgSendsTableBindings() {
        assertEquals("msg_send", MsgSends.tableName)
        // touch every bound column so the object initializer lines are covered
        assertNotNull(MsgSends.receiverGroupTypeDictCode)
        assertNotNull(MsgSends.receiverGroupId)
        assertNotNull(MsgSends.instanceId)
        assertNotNull(MsgSends.msgTypeDictCode)
        assertNotNull(MsgSends.localeDictCode)
        assertNotNull(MsgSends.sendStatusDictCode)
        assertNotNull(MsgSends.createTime)
        assertNotNull(MsgSends.updateTime)
        assertNotNull(MsgSends.successCount)
        assertNotNull(MsgSends.failCount)
        assertNotNull(MsgSends.jobId)
        assertNotNull(MsgSends.idempotencyKey)
        assertNotNull(MsgSends.tenantId)
    }

    @Test
    fun msgInstancesTableBindings() {
        assertEquals("msg_instance", MsgInstances.tableName)
        assertNotNull(MsgInstances.localeDictCode)
        assertNotNull(MsgInstances.title)
        assertNotNull(MsgInstances.content)
        assertNotNull(MsgInstances.templateId)
        assertNotNull(MsgInstances.sendTypeDictCode)
        assertNotNull(MsgInstances.eventTypeDictCode)
        assertNotNull(MsgInstances.msgTypeDictCode)
        assertNotNull(MsgInstances.validTimeStart)
        assertNotNull(MsgInstances.validTimeEnd)
        assertNotNull(MsgInstances.tenantId)
    }

    @Test
    fun daoNoArgConstructors() {
        assertNotNull(MsgSendDao())
        assertNotNull(MsgInstanceDao())
    }

    @Test
    fun msgSendEntityFactoryCreatesInstance() {
        val send = MsgSend().apply {
            tenantId = "t1"
            instanceId = "i1"
            msgTypeDictCode = "welcome"
            receiverGroupTypeDictCode = "user"
            sendStatusDictCode = "00"
        }
        assertEquals("t1", send.tenantId)
        assertEquals("welcome", send.msgTypeDictCode)
    }
}
