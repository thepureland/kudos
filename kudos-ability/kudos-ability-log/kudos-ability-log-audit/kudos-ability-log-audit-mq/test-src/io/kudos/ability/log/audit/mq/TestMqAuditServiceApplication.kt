package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.mq.TestMqAuditService.SaveModel
import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableKudosTest //@Import(TestMqAuditService::class)
class TestMqAuditServiceApplication {
    @Autowired
    private val service: TestMqAuditService? = null

    @Test
    fun testLog() {
        val model = SaveModel()
        model.code = "!121"
        model.setId("asda")
        service!!.saveLog()
    }
}
