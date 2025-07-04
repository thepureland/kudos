package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 字典查询记录
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictDetail : IdJsonResult<String>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2


    /** 字典类型 */
    var dictType: String? = null

    /** 字典名称 */
    var dictName: String? = null

    /** 模块编码 */
    var moduleCode: String? = null

    /** 备注 */
    var remark: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    /** 创建用户 */
    var createUser: String? = null

    /** 创建时间 */
    var createTime: LocalDateTime? = null

    /** 更新用户 */
    var updateUser: String? = null

    /** 更新时间 */
    var updateTime: LocalDateTime? = null

}