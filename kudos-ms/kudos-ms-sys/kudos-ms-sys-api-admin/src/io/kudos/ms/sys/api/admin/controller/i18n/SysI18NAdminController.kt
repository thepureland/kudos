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
 * 国际化管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/i18n")
class SysI18NAdminController :
    BaseCrudController<String, ISysI18nService, SysI18nQuery, SysI18nRow, SysI18nDetail, SysI18nEdit, SysI18nFormCreate, SysI18nFormUpdate>() {

    /**
     * 获取国际化信息
     *
     * @param locale 语言地区
     * @param i18nTypeDictCode 国际化类型字典代码
     * @param namespace 范围
     * @param atomicServiceCode 原子服务编码
     * @return Map<国际化key, 译文>
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
     * 批量获取国际化信息
     *
     * @param payload 批量i18n信息的请求载体
     * @return Map<国际化类型字典代码，Map<命名空间，Map<国际化key, 译文>>>
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
     * 更新active状态
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

}
