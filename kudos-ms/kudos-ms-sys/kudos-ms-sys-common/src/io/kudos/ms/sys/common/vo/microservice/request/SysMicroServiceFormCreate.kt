package io.kudos.ms.sys.common.vo.microservice.request


/**
 * 微服务表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceFormCreate (

    override val code: String = "",

    override val name: String = "",

    override val context: String = "",

    override val atomicService: Boolean = true,

    override val parentCode: String? = null,

    override val remark: String? = null,

) : ISysMicroServiceFormBase
