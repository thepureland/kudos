package io.kudos.ms.sys.common.vo.accessruleip.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.time.LocalDateTime

/**
 * IP访问规则表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleIpFormBase {

    /** ip起 */
    val ipStart: Long?

    /** ip止 */
    val ipEnd: Long?

    /** ip类型 */
    val ipType: Int?

    /** 过期时间 */
    val expirationDate: LocalDateTime?

    /** 父规则id */
    val parentRuleId: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?

    /** 是否启用 */
    val active: Boolean?
}
