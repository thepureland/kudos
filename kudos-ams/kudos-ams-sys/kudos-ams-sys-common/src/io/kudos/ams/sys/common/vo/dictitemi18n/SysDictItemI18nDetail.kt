package io.kudos.ams.sys.common.vo.dictitemi18n

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 字典项国际化查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemI18nDetail (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 语言_地区 */
    var locale: String? = null,

    /** 国际化值 */
    var i18nValue: String? = null,

    /** 字典项id */
    var itemId: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}