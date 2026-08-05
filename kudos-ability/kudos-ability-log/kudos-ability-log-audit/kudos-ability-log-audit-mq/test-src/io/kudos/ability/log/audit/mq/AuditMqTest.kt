package io.kudos.ability.log.audit.mq

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import io.kudos.ability.log.audit.mq.beans.MqAuditService
import io.kudos.ability.log.audit.mq.beans.MqMonitorService
import io.kudos.ability.log.audit.mq.TestMqAuditService.SaveModel
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Wiring tests for the MQ audit service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@Import(TestMqAuditService::class)
class AuditMqTest @Autowired constructor(
    private val service: TestMqAuditService,
    private val auditService: IAuditService,
    private val monitorService: IMonitorService,
) {

    @Test
    fun testLog() {
        val model = SaveModel("asda")
        model.code = "!121"
        assertEquals("asda", model.id)
        assertEquals("!121", model.code)
        // @Audit-annotated method must complete normally even though the MQ submit is a no-op stub
        service.saveLog()
        assertEquals("code-load-success", service.load(model.code))
        assertEquals("code-load-success", service.load(null))
    }

    @Test
    fun mqAuditServiceIsPrimaryAuditService() {
        assertIs<MqAuditService>(auditService)
    }

    @Test
    fun mqMonitorServiceIsPrimaryMonitorService() {
        assertIs<MqMonitorService>(monitorService)
        assertTrue(monitorService.submit(SysMonitorMsgVo()))
    }

    @Test
    fun submitKeepsMqProducerMetadata() {
        val submitMethod = MqAuditService::class.java.getMethod("submit", SysAuditLogModel::class.java)
        val producer = submitMethod.getAnnotation(MqProducer::class.java)

        assertEquals("LOG_AUDIT_TOPIC", producer.topic)
        assertEquals("logAudit-out-0", producer.bindingName)
    }

    @Test
    fun submitReturnsTruePlaceholder() {
        assertTrue(auditService.submit(SysAuditLogModel()))
    }

}
