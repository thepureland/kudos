package io.kudos.ms.sys.common.vo.system.request


/**
 * 系统表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemFormCreate (

    override val code: String = "",

    override val name: String = "",

    override val subSystem: Boolean = true,

    override val parentCode: String? = null,

    override val remark: String? = null,

) : ISysSystemFormBase
