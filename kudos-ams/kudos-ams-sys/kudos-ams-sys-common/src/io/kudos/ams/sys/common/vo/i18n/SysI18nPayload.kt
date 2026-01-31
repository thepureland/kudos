package io.kudos.ams.sys.common.vo.i18n

import io.kudos.base.support.payload.FormPayload


/**
 * 国际化表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 语言_地区 */
    var locale: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 国际化类型字典代码 */
    var i18nTypeDictCode: String? = null,

    /** 国际化key */
    var key: String? = null,

    /** 国际化值 */
    var value: String? = null,

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
