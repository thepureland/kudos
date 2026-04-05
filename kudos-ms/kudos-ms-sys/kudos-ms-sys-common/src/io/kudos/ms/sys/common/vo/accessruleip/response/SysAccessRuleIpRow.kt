package io.kudos.ms.sys.common.vo.accessruleip.response

import java.time.LocalDateTime


/**
 * IP访问规则列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpRow (

    /** ipRule的id */
    val id: String = "",

    /** ip起 */
    val ipStart: Long? = null,

    /** ip止 */
    val ipEnd: Long? = null,

    /** ip类型字典代码 */
    val ipTypeDictCode: String? = null,

    /** 过期时间 */
    val expirationTime: LocalDateTime? = null,

    /** 父规则id */
    val parentRuleId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 父规则是否启用 */
    val parentRuleActive: Boolean? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型字典代码 */
    val accessRuleTypeDictCode: String? = null,

)