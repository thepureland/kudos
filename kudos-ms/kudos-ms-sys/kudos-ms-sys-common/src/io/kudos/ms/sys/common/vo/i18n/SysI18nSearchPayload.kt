package io.kudos.ms.sys.common.vo.i18n

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 国际化查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nSearchPayload (

    //region your codes 1

    /** 语言_地区 */
    val locale: String? = null,

    /** 原子服务编码 */
    val atomicServiceCode: String? = null,

    /** 国际化类型字典代码 */
    val i18nTypeDictCode: String? = null,

    /** 国际化命名空间 */
    val namespace: String? = null,

    /** 国际化key */
    val key: String? = null,

    /** 国际化值 */
    val value: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysI18nRecord::class

    //endregion your codes 3

}
