package io.kudos.ms.sys.common.param.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 参数详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamDetail (

    /** 主键 */
    override val id: String = "",


    /** 参数名称 */
    val paramName: String = "",

    /** 参数值 */
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

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