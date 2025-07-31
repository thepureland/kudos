package io.kudos.ams.sys.common.vo.dictitemi18n

import io.kudos.base.support.payload.FormPayload


/**
 * 字典项国际化表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemI18nPayload (

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
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}