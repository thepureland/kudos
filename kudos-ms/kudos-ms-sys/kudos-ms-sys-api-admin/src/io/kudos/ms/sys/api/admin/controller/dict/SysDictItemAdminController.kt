package io.kudos.ms.sys.api.admin.controller.dict

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.dict.vo.request.SysDictItemFormCreate
import io.kudos.ms.sys.common.dict.vo.request.SysDictItemFormUpdate
import io.kudos.ms.sys.common.dict.vo.request.SysDictItemQuery
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemDetail
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemEdit
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemNode
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow
import io.kudos.ms.sys.core.dict.service.impl.VSysDictItemService
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

/**
 * Dictionary item management controller.
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
     * Return the dictionary item for the given id.
     *
     * @param id primary key
     * @return SysDictItemRow; returns null if not found
     */
    @GetMapping("/getDictItem")
    fun getDictItem(id: String): SysDictItemCacheEntry? {
        return vSysDictItemService.getFromCache(id)
    }

    /**
     * Paged query; `parentCode` of matched rows is back-filled internally by the service.
     *
     * @param sysDictItemQuery query parameter container
     * @return PagingSearchResult<SysDictItemRow>
     */
    @PostMapping("/pagingSearchDictItem")
    fun pagingSearchDict(@RequestBody sysDictItemQuery: SysDictItemQuery): PagingSearchResult<SysDictItemRow> =
        vSysDictItemService.pagingSearchWithParentCode(sysDictItemQuery)

    /**
     * Return direct children (first-level dictionary items) of the given dictionary type.
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @param activeOnly active only; false includes inactive ones
     * @return List<SysDictItemNode>
     */
    @GetMapping("/getDirectChildrenOfDict")
    fun getDirectChildrenOfDict(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true
    ): List<SysDictItemNode> =
        sysDictItemService.getDirectChildrenOfDictAsNodes(atomicServiceCode, dictType, activeOnly)

    /**
     * Return direct children of the given dictionary item.
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @param itemCode dictionary item code
     * @param activeOnly active only; false includes inactive ones
     * @return List<SysDictItemNode>
     */
    @GetMapping("/getDirectChildrenOfItem")
    fun getDirectChildrenOfItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true
    ): List<SysDictItemNode> =
        sysDictItemService.getDirectChildrenOfItemAsNodes(atomicServiceCode, dictType, itemCode, activeOnly)

    /**
     * Get dictionary items by dictionary type and atomic service code.
     *
     * @param dictType dictionary type
     * @param atomicServiceCode atomic service code
     * @return list of dictionary items
     */
    @GetMapping("/getDictItems")
    fun getDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> {
        return sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
    }

    /**
     * Batch get dictionary item information.
     *
     * @param dictTypesByAtomicServiceCode Map<atomic service code, Collection<dictionary type code>>
     * @return Map<atomic service code, Map<dictionary type, list of dictionary item cache entries>>
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
     * Get dictionary items by dictionary type and atomic service code.
     *
     * @param dictType dictionary type
     * @param atomicServiceCode atomic service code
     * @return LinkedHashMap<dictionary item code, item translation or its i18n key>
     */
    @GetMapping("/getDictItemMap")
    fun getDictItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> {
        return sysDictItemService.getDictItemMapFromCache(dictType, atomicServiceCode)
    }

    /**
     * Batch get dictionary item information.
     *
     * @param dictTypesByAtomicServiceCode Map<atomic service code, Collection<dictionary type code>>
     * @return Map<atomic service code, Map<dictionary type, LinkedHashMap<dictionary item code, item translation or its i18n key>>>
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
