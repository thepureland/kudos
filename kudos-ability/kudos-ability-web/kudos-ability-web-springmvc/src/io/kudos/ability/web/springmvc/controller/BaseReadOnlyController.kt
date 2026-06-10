package io.kudos.ability.web.springmvc.controller

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.base.model.payload.ListSearchPayload
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.reflect.KClass


/**
 * Base read-only Controller.
 *
 * @param PK primary key type
 * @param B business service class
 * @param S list query condition VO class (request)
 * @param R list row VO class (response)
 * @param D detail VO class (response)
 * @author K
 * @author AI: Codex
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

    /** Detail VO class, resolved lazily from the subclass's generic parameter (index 4). */
    @Suppress("UNCHECKED_CAST")
    private val detailVoClass: KClass<D> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 4) as KClass<D>
    }

    /**
     * Paged list query.
     *
     * @param searchPayload list query condition VO
     * @return wraps the current page's result records and the total record count
     */
    @PostMapping("/pagingSearch")
    open fun pagingSearch(@RequestBody searchPayload: S): PagingSearchResult<R> {
        @Suppress("UNCHECKED_CAST")
        return service.pagingSearch(searchPayload) as PagingSearchResult<R>
    }

    /**
     * Return the detail of the record with the given primary key.
     *
     * @return WebResult(record detail)
     */
    @GetMapping("/getDetail")
    open fun getDetail(id: PK): D =
        service.get(id, detailVoClass) ?: throw ObjectNotFoundException("Record not found!")

}