package io.kudos.ms.sys.common.system.vo.request

/**
 * System create form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemFormCreate (

    override val code: String ,

    override val name: String ,

    override val subSystem: Boolean ,

    override val parentCode: String? ,

    override val remark: String? ,

) : ISysSystemFormBase
