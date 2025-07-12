package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive


/**
 * 字典表单载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictPayload : FormPayload<String>() {
//endregion your codes 1

    //region your codes 2

    /** 字典id */
    var dictId: String? = null

    /** 字典类型 */
    var dictType: String? = null

    /** 父项id */
    var parentId: String? = null

    /** 模块编码 */
    var moduleCode: String? = null

    /** 编码 */
    @get:NotBlank(message = "编码不能为空！")
    var code: String? = null

    /** 名称 */
    @get:NotBlank(message = "名称不能为空！")
    var name: String? = null

    /** 序号 */
    @get:Positive(message = "序号必须为正数！")
    @get:Digits(integer = 9, fraction = 0, message = "序号必须为整数！")
    @get:Max(value = 999999999, message = "序号不能大于999999999！")
    var seqNo: Int? = null

    /** 备注 */
    var remark: String? = null

    //endregion your codes 2

}