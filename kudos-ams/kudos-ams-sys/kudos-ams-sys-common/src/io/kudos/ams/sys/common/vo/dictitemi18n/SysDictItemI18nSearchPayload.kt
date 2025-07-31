package io.kudos.ams.sys.common.vo.dictitemi18n

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 字典项国际化查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemI18nSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysDictItemI18nRecord::class,

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
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysDictItemI18nRecord::class)

    //endregion your codes 3

}