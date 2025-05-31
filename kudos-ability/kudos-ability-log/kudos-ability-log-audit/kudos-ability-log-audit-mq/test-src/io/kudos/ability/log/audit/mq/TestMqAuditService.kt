package io.kudos.ability.log.audit.mq

import org.soul.ability.log.audit.common.annotation.Audit
import org.soul.base.dicts.DictOpType
import org.soul.base.support.model.common.BaseEditModel
import org.springframework.stereotype.Component

@Component
class TestMqAuditService {

    @Audit(opType = DictOpType.CREATE, moduleCode = "AAA：sys_module表的code")
    fun saveLog() {
        println(1111)
    }

    fun load(code: String?): String {
        return "code-load-success"
    }

    class SaveModel : BaseEditModel<String?>() {
        @JvmField
        var code: String? = null
    }

}
