package io.kudos.ms.sys.common.i18n.vo.request

/**
 * Request VO for creating an i18n form.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nFormCreate (

    override val locale: String ,

    override val atomicServiceCode: String ,

    override val i18nTypeDictCode: String ,

    override val namespace: String ,

    override val key: String ,

    override val value: String ,

    override val remark: String? ,

) : ISysI18nFormBase
