package io.kudos.ms.sys.common.tenant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Tenant edit response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantEdit (

    /** Primary key */
    override val id: String = "",


    /** Name */
    val name: String = "",

    /** Timezone */
    val timezone: String? = null,

    /** Default language code */
    val defaultLanguageCode: String? = null,

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


    /** Comma-separated subsystem codes */
    var subSystemCodes: String = ""
}