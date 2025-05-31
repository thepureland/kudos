package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.mq.TestMqAuditService.SaveModel
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@EnableKudosTest
@Import(TestMqAuditService::class)
class TestMqAuditServiceApplication {

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
