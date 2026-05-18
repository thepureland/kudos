package io.kudos.ms.sys.common.outline.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 出网白名单详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineDetail(

    /** 主键 */
    override val id: String = "",

    /** 名称 */
    val name: String = "",

    /** 主机名或通配符 */
    val host: String = "",

    /** 端口 */
    val port: Int? = null,

    /** 协议 */
    val protocol: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 租户id */
    val tenantId: String? = null,

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
