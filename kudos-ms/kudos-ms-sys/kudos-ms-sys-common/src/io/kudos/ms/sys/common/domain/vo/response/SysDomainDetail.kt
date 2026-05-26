package io.kudos.ms.sys.common.domain.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Domain detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainDetail (

    /** Primary key */
    override val id: String = "",


    /** Domain */
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

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String> {


    /** Tenant name */
    var tenantName: String = ""

}