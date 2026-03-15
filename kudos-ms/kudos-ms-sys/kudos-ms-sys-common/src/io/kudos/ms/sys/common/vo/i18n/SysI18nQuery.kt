package io.kudos.ms.sys.common.vo.i18n

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KProperty0


/**
 * 国际化查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nQuery (


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

    /** 仅启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {

    constructor() : this("")

    override fun getReturnEntityClass() = SysI18nRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::key to OperatorEnum.ILIKE,
        ::namespace to OperatorEnum.ILIKE,
    )


}
