package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.i18n.SysI18nForm
import io.kudos.ms.sys.common.vo.i18n.SysI18nRow


/**
 * 国际化 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysI18nApi {
//endregion your codes 1

    //region your codes 2

    fun getI18nValue(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String?

    fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String
    ): Map<String, String>

    fun batchSaveOrUpdate(i18ns: List<SysI18nForm>): Int

    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}