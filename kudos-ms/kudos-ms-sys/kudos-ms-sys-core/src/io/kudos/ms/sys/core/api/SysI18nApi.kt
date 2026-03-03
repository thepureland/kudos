package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysI18nApi
import io.kudos.ms.sys.common.vo.i18n.SysI18nPayload
import io.kudos.ms.sys.common.vo.i18n.SysI18nRecord
import io.kudos.ms.sys.core.service.iservice.ISysI18nService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 国际化 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysI18nApi : ISysI18nApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysI18nService: ISysI18nService

    override fun getI18nValue(locale: String, atomicServiceCode: String, i18nTypeDictCode: String, key: String): String? {
        return sysI18nService.getI18nValue(locale, atomicServiceCode, i18nTypeDictCode, key)
    }

    override fun getI18nsByAtomicServiceAndType(
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        locale: String?
    ): List<SysI18nRecord> {
        return sysI18nService.getI18nsByAtomicServiceAndType(atomicServiceCode, i18nTypeDictCode, locale)
    }

    override fun batchSaveOrUpdate(i18ns: List<SysI18nPayload>): Int {
        return sysI18nService.batchSaveOrUpdate(i18ns)
    }

    override fun updateActive(id: String, active: Boolean): Boolean {
        return sysI18nService.updateActive(id, active)
    }

    //endregion your codes 2

}