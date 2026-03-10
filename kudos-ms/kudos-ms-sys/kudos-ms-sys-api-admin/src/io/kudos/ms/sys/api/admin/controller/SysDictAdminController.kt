package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.dict.SysDictDetail
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictRow
import io.kudos.ms.sys.common.vo.dict.SysDictQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.service.impl.SysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * 字典管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dict")
class SysDictAdminController :
    BaseCrudController<String, ISysDictService, SysDictQuery, SysDictRow, SysDictDetail, SysDictForm>() {

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService

    /**
     * 根据字典类型和原子服务编码取得对应的字典项
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return 字典项列表
     */
    @GetMapping("/getDictItems")
    fun getDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemService.getItems(dictType, atomicServiceCode)
    }

    /**
     * 批量获取字典项信息
     *
     * @param dictTypesByAtomicServiceCode Map<原子服务编码，Collection<字典类型编码>>
     * @return Map<原子服务编码, Map<字典类型，字典项缓存对象列表>>
     */
    @PostMapping("/batchGetDictItems")
    @ResponseBody
    fun batchGetDictItems(
        @RequestBody
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>> {
       return sysDictItemService.batchGetDictItems(dictTypesByAtomicServiceCode)
    }

    /**
     * 根据字典类型和原子服务编码取得对应的字典项
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return LinkedHashMap<字典项编码，字典项译文或其国际化key>
     */
    @GetMapping("/getDictItemMap")
    fun getDictItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> {
        return sysDictItemService.getItemMap(dictType, atomicServiceCode)
    }

    /**
     * 批量获取字典项信息
     *
     * @param dictTypesByAtomicServiceCode Map<原子服务编码，Collection<字典类型编码>>
     * @return Map<原子服务编码，Map<字典类型，LinkedHashMap<字典项编码，字典项译文或其国际化key>>>
     */
    @PostMapping("/batchGetDictItemMap")
    @ResponseBody
    fun batchGetDictItemMap(
        @RequestBody
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>> {
        return sysDictItemService.batchGetDictItemMap(dictTypesByAtomicServiceCode)
    }

}