package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow
import kotlin.reflect.KProperty0


/**
 * Dictionary item list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemQuery (

    /** Primary key */
    val id: String? = null,

    /** Dictionary item code */
    val itemCode: String? = null,

    /** Dictionary item name */
    val itemName: String? = null,

    /** Dictionary id */
    val dictId: String? = null,

    /** Dictionary item order number */
    val orderNum: Int? = null,

    /** Parent id */
    var parentId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    var active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Dictionary type */
    val dictType: String? = null,

    /** Dictionary name */
    val dictName: String? = null,

    /** Atomic service code */
    var atomicServiceCode: String? = null,

    /** Whether dictionary is active */
    val dictActive: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDictItemRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::dictType to OperatorEnum.ILIKE,
        ::dictName to OperatorEnum.ILIKE,
        ::itemCode to OperatorEnum.ILIKE,
        ::itemName to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}