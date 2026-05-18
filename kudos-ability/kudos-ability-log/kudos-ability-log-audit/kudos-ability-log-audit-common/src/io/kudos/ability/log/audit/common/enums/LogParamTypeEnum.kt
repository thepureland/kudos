package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * [io.kudos.ability.log.audit.common.entity.LogParamVo.type] 取值枚举——决定描述参数
 * 在最终格式化时的处理方式（字符串原样 / 按 locale 货币 / 按 locale 日期）。
 *
 * @author K
 * @since 1.0.0
 */
enum class LogParamTypeEnum(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    STRING("1", "字符串"),
    CURRENCY("2", "货币"),
    DATE("3", "日期");

}

