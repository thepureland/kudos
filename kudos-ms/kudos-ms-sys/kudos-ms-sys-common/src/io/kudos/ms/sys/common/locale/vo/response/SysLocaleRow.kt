package io.kudos.ms.sys.common.locale.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for language dictionary list query result.
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleRow(

    /** Primary key */
    override val id: String = "",

    /** Language code */
    val code: String = "",

    /** Display name */
    val displayName: String = "",

    /** English name */
    val englishName: String = "",

    /** Sort number */
    val sortNo: Int = 0,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

    /** Creation time */
    val createTime: LocalDateTime? = null,
) : IIdEntity<String>
