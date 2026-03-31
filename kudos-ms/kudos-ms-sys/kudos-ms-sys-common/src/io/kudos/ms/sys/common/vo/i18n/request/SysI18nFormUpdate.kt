package io.kudos.ms.sys.common.vo.i18n.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 国际化表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val locale: String = "",

    override val atomicServiceCode: String = "",

    override val i18nTypeDictCode: String = "",

    override val namespace: String = "",

    override val key: String = "",

    override val value: String = "",

    override val remark: String? = null,

) : IIdEntity<String?>, ISysI18nFormBase
