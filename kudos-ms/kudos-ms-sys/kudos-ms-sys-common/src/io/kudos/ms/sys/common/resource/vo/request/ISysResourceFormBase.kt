package io.kudos.ms.sys.common.resource.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

/**
 * Resource form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceFormBase {

    /** Name */
    @get:NotBlank
    @get:MaxLength(64)
    val name: String

    /** url */
    @get:MaxLength(256)
    @get:Matches(RegExpEnum.CONTEXT)
    val url: String?

    /** Resource type dict code */
    @get:NotBlank
    @get:FixedLength(1)
    @get:DictItemCode(dictType = SysDictTypes.RESOURCE_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val resourceTypeDictCode: String

    /** Parent id */
    @get:FixedLength(36)
    val parentId: String?

    /** Order number among siblings under the same parent */
    val orderNum: Int?

    /** Icon */
    @get:MaxLength(256)
    val icon: String?

    /** Sub-system code */
    @get:NotBlank
    @get:MaxLength(32)
    val subSystemCode: String

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
