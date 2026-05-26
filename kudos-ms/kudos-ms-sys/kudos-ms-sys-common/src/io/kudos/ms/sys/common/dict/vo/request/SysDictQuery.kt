package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import kotlin.reflect.KProperty0


/**
 * Dictionary list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictQuery (

    val id: String? = null,

    /** Dictionary type */
    val dictType: String? = null,

    /** Dictionary name */
    val dictName: String? = null,

    /** Atomic service code */
    val atomicServiceCode: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDictRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::dictType to OperatorEnum.ILIKE,
        ::dictName to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}