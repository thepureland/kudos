package io.kudos.ms.sys.common.locale.vo.request

/**
 * 语言字典表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleFormCreate(

    override val code: String,

    override val displayName: String,

    override val englishName: String,

    override val sortNo: Int = 0,

    override val remark: String?,

) : ISysLocaleFormBase
