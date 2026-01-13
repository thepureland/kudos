package io.kudos.ability.log.audit.mq

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.base.support.IIdEntity
import org.springframework.stereotype.Component

@Component
class TestMqAuditService {

    @Audit(opType = OperationTypeEnum.CREATE, moduleCode = "AAA：sys_module表的code")
    fun saveLog() {
        println(1111)
    }

    fun load(code: String?): String {
        return "code-load-success"
    }

    class SaveModel(override var id: String?) : IIdEntity<String> {
        @JvmField
        var code: String? = null
    }

}
