package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.i18n.SysI18nPayload
import io.kudos.ms.sys.common.vo.i18n.SysI18nRecord


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

    fun getI18nValue(locale: String, atomicServiceCode: String, i18nTypeDictCode: String, key: String): String?

    fun getI18nsByAtomicServiceAndType(atomicServiceCode: String, i18nTypeDictCode: String, locale: String? = null): List<SysI18nRecord>

    fun batchSaveOrUpdate(i18ns: List<SysI18nPayload>): Int

    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}