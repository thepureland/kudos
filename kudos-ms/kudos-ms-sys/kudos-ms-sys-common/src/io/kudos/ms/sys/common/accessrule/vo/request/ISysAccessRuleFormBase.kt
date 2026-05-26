package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank

/**
 * Access rule form base fields (shared by create / update).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysAccessRuleFormBase {

    /** Access rule type dict code (column `access_rule_type_dict_code`) */
    @get:NotBlank
    @get:MaxLength(32)
    @get:DictItemCode(dictType = SysDictTypes.ACCESS_RULE_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val accessRuleTypeDictCode: String

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
