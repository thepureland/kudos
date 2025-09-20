package io.kudos.ability.log.audit.mq

import io.kudos.base.enums.ienums.IDictEnum


enum class TestModuleEnum(
    override val code: String,
    override val trans: String
) : IDictEnum {

    DEMO("demo", "测试模块");

    companion object {
        const val MODULE_DEMO: String = "demo"
    }
}
