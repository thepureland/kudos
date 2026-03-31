package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.common.vo.dict.request.ISysDictFormCreate
import io.kudos.ms.sys.common.vo.dict.request.ISysDictFormUpdate
import io.kudos.ms.sys.common.vo.dict.request.SysDictQuery
import io.kudos.ms.sys.common.vo.dict.response.SysDictDetail
import io.kudos.ms.sys.common.vo.dict.response.SysDictEdit
import io.kudos.ms.sys.common.vo.dict.response.SysDictRow
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import org.springframework.web.bind.annotation.*

/**
 * 字典管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dict")
class SysDictAdminController :
    BaseCrudController<String, ISysDictService, SysDictQuery, SysDictRow, SysDictDetail, SysDictEdit, ISysDictFormCreate, ISysDictFormUpdate>() {




    /**
     * 返回指定id的字典
     *
     * @param id 主键
     * @return SysDictItemRow，找不到返回null
     */
    @GetMapping("/getDict")
    fun getDict(id: String): SysDictCacheEntry? {
        return service.getDictFromCache(id)
    }

    /**
     * 分页查询
     *
     * @param sysDictQuery 查询参数载体
     * @return PagingSearchResult<SysDictRow>
     */
    @PostMapping("/pagingSearchDict")
    @Suppress("UNCHECKED_CAST")
    fun pagingSearchDict(@RequestBody sysDictQuery: SysDictQuery): PagingSearchResult<SysDictRow> {
        return service.pagingSearch(sysDictQuery) as PagingSearchResult<SysDictRow>
    }

    /**
     * 返回原子服务编码对应的所有字典类型
     *
     * @param atomicServiceCode 原子服务编码
     * @param activeOnly 仅启用，为null或false将包含未启用的
     * @return Map<主键，字典类型>
     */
    @GetMapping("/getDictTypesByAtomicServiceCode")
    fun getDictTypesByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): Map<String, String> {
        val dictTypes = service.getDictsFromCacheByAtomicServiceCode(atomicServiceCode, activeOnly)
        return dictTypes.associate { it.id to it.dictType }
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
