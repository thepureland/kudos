package io.kudos.ms.sys.common.microservice.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceRow
import kotlin.reflect.KProperty0


/**
 * Request VO for querying the microservice list.
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceQuery (

    /** Code */
    val code: String? = null,

    /** Name */
    val name: String? = null,

    /** Whether atomic service */
    val atomicService: Boolean? = null,

    /** Only enabled */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysMicroServiceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::name to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}