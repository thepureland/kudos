package io.kudos.ability.comm.email.model

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import java.io.Serial
import java.io.Serializable

/**
 * @Description 发送邮件的回调响应体
 * @Author paul
 * @Date 2023/2/9 11:15
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
