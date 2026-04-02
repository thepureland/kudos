package io.kudos.ms.sys.common.vo.resource.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank

/**
 * 资源表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceFormBase {

    /** 名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val name: String

    /** url */
    @get:MaxLength(256)
    @get:Matches(RegExpEnum.CONTEXT)
    val url: String?

    /** 资源类型字典代码 */
    @get:NotBlank
    @get:FixedLength(1)
    @get:DictItemCode(dictType = SysDictTypes.RESOURCE_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val resourceTypeDictCode: String

    /** 父id */
    @get:FixedLength(36)
    val parentId: String?

    /** 在同父节点下的排序号 */
    val orderNum: Int?

    /** 图标 */
    val icon: String?

    /** 子系统编码 */
    @get:NotBlank
    val subSystemCode: String

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
