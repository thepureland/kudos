package io.kudos.ms.sys.common.i18n.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 国际化表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val locale: String,

    override val atomicServiceCode: String,

    override val i18nTypeDictCode: String,

    override val namespace: String,

    override val key: String,

    override val value: String,

    override val remark: String?,

) : IIdEntity<String>, ISysI18nFormBase
