package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank

/**
 * 国际化表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysI18nFormBase {

    /** 语言_地区 */
    @get:NotBlank
    @get:FixedLength(5)
    val locale: String

    /** 原子服务编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val atomicServiceCode: String

    /** 国际化类型字典代码 */
    @get:NotBlank
    @get:MaxLength(32)
    @get:DictItemCode(dictType = SysDictTypes.I18N_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val i18nTypeDictCode: String

    /** 国际化命名空间 */
    @get:NotBlank
    @get:MaxLength(128)
    val namespace: String

    /** 国际化key */
    @get:NotBlank
    @get:MaxLength(128)
    val key: String

    /** 国际化值 */
    @get:NotBlank
    @get:MaxLength(1000)
    val value: String

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
