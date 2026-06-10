package io.kudos.ms.sys.api.admin.controller.dict

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.dict.vo.request.ISysDictFormCreate
import io.kudos.ms.sys.common.dict.vo.request.ISysDictFormUpdate
import io.kudos.ms.sys.common.dict.vo.request.SysDictQuery
import io.kudos.ms.sys.common.dict.vo.response.SysDictDetail
import io.kudos.ms.sys.common.dict.vo.response.SysDictEdit
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import org.springframework.web.bind.annotation.*

/**
 * Dictionary management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dict")
class SysDictAdminController :
    BaseCrudController<String, ISysDictService, SysDictQuery, SysDictRow, SysDictDetail, SysDictEdit, ISysDictFormCreate, ISysDictFormUpdate>() {

    /**
     * Return the dictionary for the given id.
     *
     * @param id primary key
     * @return SysDictCacheEntry; returns null if not found
     */
    @GetMapping("/getDict")
    fun getDict(id: String): SysDictCacheEntry? {
        return service.getDictFromCache(id)
    }

    /**
     * Paged query.
     *
     * @param sysDictQuery query parameter container
     * @return PagingSearchResult<SysDictRow>
     */
    @PostMapping("/pagingSearchDict")
    @Suppress("UNCHECKED_CAST")
    fun pagingSearchDict(@RequestBody sysDictQuery: SysDictQuery): PagingSearchResult<SysDictRow> {
        return service.pagingSearch(sysDictQuery) as PagingSearchResult<SysDictRow>
    }

    /**
     * Return all dictionary types for the given atomic service code.
     *
     * @param atomicServiceCode atomic service code
     * @param activeOnly active only; null or false includes inactive ones
     * @return Map<primary key, dictionary type>
     */
    @GetMapping("/getDictTypesByAtomicServiceCode")
    fun getDictTypesByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): Map<String, String> =
        service.getDictTypesByAtomicServiceCode(atomicServiceCode, activeOnly)

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
