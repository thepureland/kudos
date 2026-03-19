package io.kudos.ability.web.springmvc.controller

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.iservice.IBaseReadOnlyService
import io.kudos.base.model.payload.ListSearchPayload
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.reflect.KClass


/**
 * 基础的只读Controller
 *
 * @param PK 主键类型
 * @param B 业务处理类
 * @param S 列表查询条件VO类(请求)
 * @param R 列表行VO类(响应)
 * @param D 详情Vo类(响应)
 * @author K
 * @since 1.0.0
 */
open class BaseReadOnlyController<
        PK : Any,
        B : IBaseReadOnlyService<PK, *>,
        S : ListSearchPayload,
        R : Any,
        D: Any>
    : BaseController() {

    @Autowired
    protected lateinit var service: B

    private var detailVoClass: KClass<D>? = null

    /**
     * 列表分页查询
     *
     * @param searchPayload 列表查询条件VO
     * @return 封装当前分页的结果记录和总记录数
     */
    @PostMapping("/pagingSearch")
    open fun pagingSearch(@RequestBody searchPayload: S): PagingSearchResult<R> {
        @Suppress("UNCHECKED_CAST")
        return service.pagingSearch(searchPayload) as PagingSearchResult<R>
    }

    /**
     * 返回指定主键的记录详情
     *
     * @return WebResult(记录详情)
     */
    @GetMapping("/getDetail")
    open fun getDetail(id: PK): D {
        if (detailVoClass == null) {
            @Suppress("UNCHECKED_CAST")
            detailVoClass = GenericKit.getSuperClassGenricClass(this::class, 4) as KClass<D>
        }
        return service.get(id, requireNotNull(detailVoClass) { "detailVoClass is null" }) ?: throw ObjectNotFoundException("找不到记录！")
    }

}