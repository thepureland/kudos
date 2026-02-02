package io.kudos.ms.sys.common.vo.i18n

import io.kudos.base.support.result.IdJsonResult


/**
 * 国际化查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nRecord (

    //region your codes 1

    /** 主键 */
    override var id: String? = null,

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

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}
