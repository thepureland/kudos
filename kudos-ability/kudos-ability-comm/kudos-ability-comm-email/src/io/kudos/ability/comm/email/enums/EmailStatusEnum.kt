package io.kudos.ability.comm.email.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * 邮件发送状态
 */
enum class EmailStatusEnum(
    override val code: String,
    override val trans: String
) : IDictEnum {

    FAIL("0", "失败"),
    SUCCESS_PART("1", "部分成功"),
    SUCCESS("2", "成功");

}
