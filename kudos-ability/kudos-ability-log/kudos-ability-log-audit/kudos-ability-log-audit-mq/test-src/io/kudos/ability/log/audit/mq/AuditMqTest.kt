package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.mq.TestMqAuditService.SaveModel
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import kotlin.test.Test

@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@Import(TestMqAuditService::class)
class AuditMqTest {

    @Autowired
    private lateinit var service: TestMqAuditService

    @Test
    fun testLog() {
        val model = SaveModel()
        model.code = "!121"
        model.setId("asda")
        service.saveLog()
    }

}
