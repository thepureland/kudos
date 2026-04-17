package io.kudos.ms.sys.common.system.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 系统详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemDetail (

    override val id: String = "",

    /** 编码 */
    val code: String = "",

    /** 名称 */
    val name: String = "",

    /** 是否子系统 */
    val subSystem: Boolean = true,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>