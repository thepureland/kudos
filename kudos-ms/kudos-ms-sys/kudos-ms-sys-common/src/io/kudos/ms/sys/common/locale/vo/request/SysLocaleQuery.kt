package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleRow
import kotlin.reflect.KProperty0


/**
 * Request VO for querying the language dictionary list.
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleQuery(

    /** Language code */
    val code: String? = null,

    /** Display name */
    val displayName: String? = null,

    /** Only enabled */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysLocaleRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::displayName to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}
