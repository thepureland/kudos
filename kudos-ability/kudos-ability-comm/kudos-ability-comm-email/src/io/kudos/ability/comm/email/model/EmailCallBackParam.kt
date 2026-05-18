package io.kudos.ability.comm.email.model

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import java.io.Serial
import java.io.Serializable

/**
 * 邮件发送回调载体。`EmailHandler.send(...)` 的 callback 收到本对象——业务侧据此
 * 写"邮件发送记录"或触发重发。
 *
 * - [status] `SUCCESS` —— 全部收件人都发出
 * - [status] `SUCCESS_PART` —— 部分收件人发出，[successEmails] / [failEmails] 各非空
 * - [status] `FAIL` —— 全部失败（[failEmails] 即为原 receivers）
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class EmailCallBackParam : Serializable {
    /**
     * 发送状态
     */
    var status: EmailStatusEnum? = null

    /**
     * 发送成功的邮箱帐号
     */
    var successEmails: MutableSet<String>? = null

    /**
     * 发送失败的邮箱帐号
     */
    var failEmails: MutableSet<String>? = null

    override fun toString(): String {
        return "EmailCallBackParam{" +
                "status=" + status +
                ", successEmails=" + successEmails +
                ", failEmails=" + failEmails +
                '}'
    }

    companion object {
        @Serial
        private val serialVersionUID = -1651796105092981458L
    }
}
