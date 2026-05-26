package io.kudos.ms.sys.common.locale.vo.request

/**
 * Request VO for creating a language dictionary form.
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
