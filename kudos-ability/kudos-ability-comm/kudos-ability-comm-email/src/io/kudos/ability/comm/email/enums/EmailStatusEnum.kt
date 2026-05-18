package io.kudos.ability.comm.email.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * 邮件发送结果状态。来源于 JavaMail `SendFailedException` 的细分：
 *  - 全部发送出去 → [SUCCESS]
 *  - 有 `validSentAddresses` **且** 有 `validUnsentAddresses`/`invalidAddresses` → [SUCCESS_PART]
 *  - 没有任何收件人成功 → [FAIL]
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
enum class EmailStatusEnum(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    /** 全部收件人都失败。 */
    FAIL("0", "失败"),

    /** 部分收件人成功、部分失败——业务侧通常需要把失败列表入失败队列重发。 */
    SUCCESS_PART("1", "部分成功"),

    /** 全部收件人都成功。 */
    SUCCESS("2", "成功");

}
