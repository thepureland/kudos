package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.common.vo.dict.SysDictDetail
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictRow
import io.kudos.ms.sys.common.vo.dict.SysDictQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.core.service.impl.VSysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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




    /**
     * 返回指定id的字典
     *
     * @param id 主键
     * @return SysDictItemRow，找不到返回null
     */
    @GetMapping("/getDict")
    fun getDict(id: String): SysDictCacheEntry? {
        return service.getFromCache(id)
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
        val dictTypes = service.getDictsByAtomicServiceCode(atomicServiceCode, activeOnly)
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