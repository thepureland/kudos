package io.kudos.ms.sys.api.admin.controller.i18n

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nBatchPayload
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormCreate
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nQuery
import io.kudos.ms.sys.common.i18n.vo.response.SysI18nDetail
import io.kudos.ms.sys.common.i18n.vo.response.SysI18nEdit
import io.kudos.ms.sys.common.i18n.vo.response.SysI18nRow
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import org.springframework.web.bind.annotation.*

/**
 * Internationalization management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/i18n")
class SysI18NAdminController :
    BaseCrudController<String, ISysI18nService, SysI18nQuery, SysI18nRow, SysI18nDetail, SysI18nEdit, SysI18nFormCreate, SysI18nFormUpdate>() {

    /**
     * Get internationalization information.
     *
     * @param locale language/region
     * @param i18nTypeDictCode i18n type dictionary code
     * @param namespace namespace
     * @param atomicServiceCode atomic service code
     * @return Map<i18n key, translation>
     */
    @GetMapping("/getI18ns")
    fun getI18ns(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String> {
        return service.getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)
    }

    /**
     * Batch get internationalization information.
     *
     * @param payload request container for batch i18n information
     * @return Map<i18n type dictionary code, Map<namespace, Map<i18n key, translation>>>
     */
    @PostMapping("/batchGetI18ns")
    @ResponseBody
    fun batchGetI18ns(
        @RequestBody
        payload: SysI18nBatchPayload
    ): Map<String, Map<String, Map<String, String>>> {
        return service.batchGetI18nsFromCache(
            payload.locale,
            payload.namespacesByI18nTypeDictCode,
            payload.atomicServiceCodes
        )
    }

    /**
     * Update the active status.
     *
     * @param id primary key
     * @param active whether enabled
     * @return whether the update succeeded
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

}
