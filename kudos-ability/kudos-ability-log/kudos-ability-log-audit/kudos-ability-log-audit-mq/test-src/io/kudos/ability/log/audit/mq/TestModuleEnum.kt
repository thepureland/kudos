package io.kudos.ability.log.audit.mq

import org.soul.base.ienums.ICodeEnum

enum class TestModuleEnum(code: String, trans: String) : ICodeEnum {

    DEMO("demo", "测试模块");

    private val code: String?
    private val trans: String?

    init {
        this.code = code
        this.trans = trans
    }

    override fun getCode(): String? {
        return code
    }

    override fun getTrans(): String? {
        return trans
    }

    companion object {
        const val MODULE_DEMO: String = "demo"
    }
}
