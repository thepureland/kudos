package io.kudos.ams.sys.common.vo.dictitemi18n

import io.kudos.base.support.result.IdJsonResult


/**
 * 字典项国际化查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemI18nRecord (

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

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}