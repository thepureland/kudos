package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IDictEnum

/**
 * 性别枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class SexEnum(override val code: String, override val trans: String) : IDictEnum {

    FEMALE("0", "女性"),
    MALE("1", "男性"),
    SECRET("9", "保密");

}