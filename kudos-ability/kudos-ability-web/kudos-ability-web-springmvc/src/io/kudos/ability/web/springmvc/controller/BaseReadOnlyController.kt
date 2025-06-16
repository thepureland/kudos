package io.kudos.ability.web.springmvc.controller

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.support.biz.IBaseReadOnlyBiz
import io.kudos.base.support.payload.FormPayload
import io.kudos.base.support.payload.ListSearchPayload
import org.soul.base.support.result.IJsonResult
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
 * @param S 列表查询条件载体类
 * @param R 记录类型
 * @param F 表单实体类
 * @author K
 * @since 1.0.0
 */
open class BaseReadOnlyController<PK : Any, B : IBaseReadOnlyBiz<PK, *>, S : ListSearchPayload, R : IJsonResult, D : IJsonResult, F : FormPayload<PK>>
    : BaseController<F>() {

    @Autowired
    protected lateinit var biz: B

    private var resultClass: KClass<F>? = null
    private var detailClass: KClass<D>? = null

    /**
     * 列表查询
     *
     * @param searchPayload 列表查询条件载体
     * @return Pair(记录列表，总记录数)
     * @author K
     * @since 1.0.0
     */
    @PostMapping("/search")
    @Suppress("UNCHECKED_CAST")
    open fun search(@RequestBody searchPayload: S): Pair<List<R>, Int> {
        return biz.pagingSearch(searchPayload) as Pair<List<R>, Int>
    }

    /**
     * 返回指定主键的记录
     *
     * @return Payload
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/get")
    @Suppress("UNCHECKED_CAST")
    open fun get(id: PK): F {
        if (resultClass == null) {
            resultClass = GenericKit.getSuperClassGenricClass(this::class, 5) as KClass<F>
        }
        return biz.get(id, resultClass!!) ?: throw ObjectNotFoundException("找不到记录！")
    }

    /**
     * 返回指定主键的记录详情
     *
     * @return WebResult(记录详情)
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/getDetail")
    @Suppress("UNCHECKED_CAST")
    open fun getDetail(id: PK): D {
        if (detailClass == null) {
            detailClass = GenericKit.getSuperClassGenricClass(this::class, 4) as KClass<D>
        }
        return biz.get(id, detailClass!!) ?: throw ObjectNotFoundException("找不到记录！")
    }

}