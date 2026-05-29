package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MqMonitorService].
 *
 * The implementation is intentionally a no-op stub — the real MQ send happens in the AOP aspect
 * keyed on the [MqProducer] annotation. We can't run the aspect here without a Spring context, so
 * the tests pin the **annotation metadata** and the **return value**:
 *  - `topic` and `bindingName` must match the binding configured in `kudos-ability-log-audit-mq.yml`
 *    (`logMonitor-out-0` → `LOG_MONITOR_TOPIC`). A drift here means messages silently land on the
 *    wrong topic at runtime.
 *  - `submit` must always return `true` so callers don't trigger a retry loop on a fire-and-forget
 *    publish.
 */
internal class MqMonitorServiceTest {

    @Test
    fun submit_isAnnotatedWithExpectedMqProducerCoordinates() {
        val submitMethod = MqMonitorService::class.java.getDeclaredMethod("submit", SysMonitorMsgVo::class.java)
        val annotation = submitMethod.getAnnotation(MqProducer::class.java)

        assertNotNull(annotation, "submit() must carry @MqProducer or the stream-common aspect skips it and the call silently turns into a no-op")
        assertEquals(
            "LOG_MONITOR_TOPIC", annotation.topic,
            "topic must match the destination configured in kudos-ability-log-audit-mq.yml; drift here would route monitor messages to the wrong MQ subject",
        )
        assertEquals(
            "logMonitor-out-0", annotation.bindingName,
            "bindingName must match the spring-cloud-stream binding key in kudos-ability-log-audit-mq.yml",
        )
    }

    @Test
    fun submit_returnsTrueRegardlessOfVoState() {
        val service = MqMonitorService()
        // Empty VO — the no-op body shouldn't even touch the fields.
        assertTrue(service.submit(SysMonitorMsgVo()))
        // Populated VO — same contract.
        val vo = SysMonitorMsgVo().apply {
            tenantId = "t-1"
            exceptionType = "X"
        }
        assertTrue(service.submit(vo))
    }
}
