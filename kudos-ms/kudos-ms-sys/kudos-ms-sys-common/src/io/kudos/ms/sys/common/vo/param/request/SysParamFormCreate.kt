package io.kudos.ms.sys.common.vo.param.request


/**
 * 参数表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamFormCreate (

    override val paramName: String,

    override val paramValue: String,

    override val defaultValue: String?,

    override val atomicServiceCode: String,

    override val orderNum: Int?,

    override val remark: String?,

) : ISysParamFormBase
