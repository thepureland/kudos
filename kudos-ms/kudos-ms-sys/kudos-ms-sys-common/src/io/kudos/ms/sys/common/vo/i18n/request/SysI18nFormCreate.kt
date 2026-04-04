package io.kudos.ms.sys.common.vo.i18n.request


/**
 * 国际化表单新建请求VO
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
