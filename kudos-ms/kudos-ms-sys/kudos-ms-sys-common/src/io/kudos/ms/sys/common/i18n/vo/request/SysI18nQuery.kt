package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.i18n.vo.response.SysI18nRow
import kotlin.reflect.KProperty0


/**
 * Request VO for querying the i18n list.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nQuery (

    /** Language_Region */
    val locale: String? = null,

    /** Atomic service code */
    val atomicServiceCode: String? = null,

    /** I18n type dictionary code */
    val i18nTypeDictCode: String? = null,

    /** I18n namespace */
    val namespace: String? = null,

    /** I18n key */
    val key: String? = null,

    /** I18n value */
    val value: String? = null,

    /** Only enabled */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysI18nRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::key to OperatorEnum.ILIKE,
        ::namespace to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}