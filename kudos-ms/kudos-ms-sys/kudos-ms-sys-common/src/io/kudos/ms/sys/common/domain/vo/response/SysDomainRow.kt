package io.kudos.ms.sys.common.domain.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for domain list query results.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainRow (

    /** Primary key */
    override val id: String = "",

    /** Domain name */
    val domain: String = "",

    /** System code */
    val systemCode: String = "",

    /** Tenant id */
    val tenantId: String = "",

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

    /** Creation time */
    val createTime: LocalDateTime? = null,

) : IIdEntity<String> {


    /** Tenant name */
    var tenantName: String = ""


}