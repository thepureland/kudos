package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Create by (admin) on 7/10/15.
 */
enum class LogParamTypeEnum(
    override val code: String,
    override val trans: String
) : IDictEnum {

    STRING("1", "字符串"),
    CURRENCY("2", "货币"),
    DATE("3", "日期");

}

