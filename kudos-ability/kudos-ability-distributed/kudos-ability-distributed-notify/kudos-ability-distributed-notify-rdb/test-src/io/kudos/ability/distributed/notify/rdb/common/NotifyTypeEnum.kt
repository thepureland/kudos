package io.kudos.ability.distributed.notify.rdb.common

import org.soul.base.ienums.ICodeEnum

enum class NotifyTypeEnum(code: String, trans: String) : ICodeEnum {

    DS("DS", "数据源");

    private var code: String
    private var trans: String

    init {
        this.code = code
        this.trans = trans
    }

    override fun getCode(): String {
        return code
    }

    override fun getTrans(): String?{
        return trans
    }
}
