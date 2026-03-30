package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.request.SysDictItemFormCreate
import io.kudos.ms.sys.common.vo.dictitem.request.SysDictItemFormUpdate
import io.kudos.ms.sys.common.vo.dictitem.request.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.response.SysDictItemDetail
import io.kudos.ms.sys.common.vo.dictitem.response.SysDictItemEdit
import io.kudos.ms.sys.common.vo.dictitem.response.SysDictItemNode
import io.kudos.ms.sys.common.vo.dictitem.response.SysDictItemRow
import io.kudos.ms.sys.core.service.impl.VSysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

/**
 * 字典项管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dictItem")
class SysDictItemAdminController :
    BaseCrudController<String, ISysDictItemService, SysDictItemQuery, SysDictItemRow, SysDictItemDetail, SysDictItemEdit, SysDictItemFormCreate, SysDictItemFormUpdate>() {

    @Resource
    private lateinit var vSysDictItemService: VSysDictItemService

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService

    /**
     * 返回指定id的字典项
     *
     * @param id 主键
     * @return SysDictItemRow，找不到返回null
     */
    @GetMapping("/getDictItem")
    fun getDictItem(id: String): SysDictItemCacheEntry? {
        return vSysDictItemService.getFromCache(id)
    }

    /**
     * 分页查询
     *
     * @param sysDictItemQuery 查询参数载体
     * @return PagingSearchResult<SysDictItemRow>
     */
    @PostMapping("/pagingSearchDictItem")
    @Suppress("UNCHECKED_CAST")
    fun pagingSearchDict(@RequestBody sysDictItemQuery: SysDictItemQuery): PagingSearchResult<SysDictItemRow> {
        val result = vSysDictItemService.pagingSearch(sysDictItemQuery)
        val parentIds = (result.data as List<SysDictItemRow>).filter { it.parentId != null }.map { it.parentId!! }
        val map = vSysDictItemService.searchByIds(parentIds.toSet())
        if (map.isNotEmpty()) {
            (result.data as List<SysDictItemRow>).forEach { it.parentCode = map[it.id]!!.itemCode }
        }
        return result as PagingSearchResult<SysDictItemRow>
    }

    /**
     * 返回指定字典类型的直接孩子(第一层字典项)
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param activeOnly 仅启用，为false将包含未启用的
     * @return List<SysDictItemNode>
     */
    @GetMapping("/getDirectChildrenOfDict")
    fun getDirectChildrenOfDict(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true
    ): List<SysDictItemNode> {
        val cacheEntries = sysDictItemService.getDirectChildrenOfDictFromCache(atomicServiceCode, dictType, activeOnly)
        return cacheEntries.map { SysDictItemNode(it.id, it.itemCode, it.itemName) }
    }

    /**
     * 返回指定字典项的直接孩子
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @param activeOnly 仅启用，为false将包含未启用的
     * @return List<SysDictItemNode>
     */
    @GetMapping("/getDirectChildrenOfItem")
    fun getDirectChildrenOfItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true
    ): List<SysDictItemNode> {
        val cacheEntries = sysDictItemService.getDirectChildrenOfItemFromCache(atomicServiceCode, dictType, itemCode, activeOnly)
        return cacheEntries.map { SysDictItemNode(it.id, it.itemCode, it.itemName) }
    }

    /**
     * 根据字典类型和原子服务编码取得对应的字典项
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return 字典项列表
     */
    @GetMapping("/getDictItems")
    fun getDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
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
        return sysDictItemService.batchGetDictItemsFromCache(dictTypesByAtomicServiceCode)
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
        return sysDictItemService.getDictItemMapFromCache(dictType, atomicServiceCode)
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
        return sysDictItemService.batchGetDictItemMapFromCache(dictTypesByAtomicServiceCode)
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
