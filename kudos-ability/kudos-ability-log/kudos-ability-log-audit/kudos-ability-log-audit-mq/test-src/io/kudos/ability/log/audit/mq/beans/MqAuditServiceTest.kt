package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MqAuditService].
 *
 * The implementation is intentionally a no-op stub — the real MQ send happens in the AOP aspect
 * keyed on the [MqProducer] annotation. These tests pin the **annotation metadata** and the
 * **return value** (mirroring [MqMonitorServiceTest]):
 *  - `topic` and `bindingName` must match the binding configured in `kudos-ability-log-audit-mq.yml`
 *    (`logAudit-out-0` → `LOG_AUDIT_TOPIC`). Drift here means audit logs silently land on the wrong
 *    topic at runtime.
 *  - `submit` must always return `true` so callers don't trigger retry loops on a fire-and-forget
 *    publish.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MqAuditServiceTest {

    @Test
    fun submit_isAnnotatedWithExpectedMqProducerCoordinates() {
        val submitMethod = MqAuditService::class.java.getDeclaredMethod("submit", SysAuditLogModel::class.java)
        val annotation = submitMethod.getAnnotation(MqProducer::class.java)

        assertNotNull(
            annotation,
            "submit() must carry @MqProducer or the stream-common aspect skips it and the call silently turns into a no-op",
        )
        assertEquals(
            "LOG_AUDIT_TOPIC", annotation.topic,
            "topic must match the destination configured in kudos-ability-log-audit-mq.yml; drift here would route audit logs to the wrong MQ subject",
        )
        assertEquals(
            "logAudit-out-0", annotation.bindingName,
            "bindingName must match the spring-cloud-stream binding key in kudos-ability-log-audit-mq.yml",
        )
    }

    @Test
    fun submit_returnsTrueRegardlessOfModelState() {
        val service = MqAuditService()
        // Empty model — the no-op body shouldn't even touch the fields.
        assertTrue(service.submit(SysAuditLogModel()))
        // Populated model — same contract, including Unicode content.
        val model = SysAuditLogModel().apply {
            subSysCode = "子系统-測試"
            tenantId = "t-1"
        }
        assertTrue(service.submit(model))
    }

}
