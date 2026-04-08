package io.kudos.ms.sys.common.accessrule.vo.request
import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
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

    /** ip 类型字典代码（列 `ip_type_dict_code`） */
    @get:NotBlank
    @get:MaxLength(4)
    @get:DictItemCode(dictType = SysDictTypes.IP_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val ipTypeDictCode: String

    /** 过期时间 */
    val expirationDate: LocalDateTime?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?

    /** 是否启用 */
    val active: Boolean?
}
