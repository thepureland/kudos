package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Enum of values for [io.kudos.ability.log.audit.common.entity.LogParamVo.type] —
 * determines how the description parameter is processed during final formatting
 * (string as-is / locale-formatted currency / locale-formatted date).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class LogParamTypeEnum(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    STRING("1", "String"),
    CURRENCY("2", "Currency"),
    DATE("3", "Date");

}

