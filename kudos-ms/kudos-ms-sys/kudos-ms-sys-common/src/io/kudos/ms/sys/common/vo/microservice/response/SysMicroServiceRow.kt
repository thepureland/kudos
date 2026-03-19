package io.kudos.ms.sys.common.vo.microservice.response


/**
 * 微服务列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceRow (

    /** 主键 */
    val id: String = "",

    /** 编码 */
    val code: String = "",

    /** 名称 */
    val name: String = "",

    /** 上下文 */
    val context: String = "",

    /** 是否为原子服务 */
    val atomicService: Boolean = true,

    /** 父服务编码 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

)