package io.kudos.ability.web.springmvc.controller

import io.kudos.base.bean.validation.teminal.TeminalConstraintsCreator
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.support.service.iservice.IBaseCrudService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import kotlin.reflect.KClass


/**
 * 基础的增删改查Controller
 *
 * @param PK 主键类型
 * @param B 业务处理类
 * @param S 列表查询条件VO类(请求)
 * @param R 列表查询结果VO类(响应)
 * @param D 详情VO类(响应)
 * @param E 编辑VO类(响应)
 * @param CF 新增页表单VO类(请求)
 * @param UF 编辑页表单VO类(请求)
 * @author K
 * @since 1.0.0
 */
open class BaseCrudController<
        PK: Any,
        B: IBaseCrudService<PK, *>,
        S: ListSearchPayload,
        R: Any,
        D: Any,
        E: Any,
        CF: Any,
        UF: Any>
    :BaseReadOnlyController<PK, B, S, R, D>() {

    private var createFormVoClass: KClass<CF>? = null

    private var updateFormVoClass: KClass<UF>? = null

    private var editVoClass: KClass<E>? = null

    /**
     * 返回新增页表单校验规则
     *
     * @return WebResult(Map(属性名， LinkedHashMap(约束名，Array(Map(约束注解的属性名，约束注解的属性值)))))
     */
    @GetMapping("/getCreateValidationRule")
    open fun getCreateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        if (createFormVoClass == null) {
            @Suppress("UNCHECKED_CAST")
            createFormVoClass = GenericKit.getSuperClassGenricClass(this::class, 6) as KClass<CF>
        }
        return TeminalConstraintsCreator.create(requireNotNull(createFormVoClass) { "createFormVoClass is null" })
    }

    /**
     * 返回编辑页表单校验规则
     *
     * @return WebResult(Map(属性名， LinkedHashMap(约束名，Array(Map(约束注解的属性名，约束注解的属性值)))))
     */
    @GetMapping("/getUpdateValidationRule")
    open fun getUpdateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        if (updateFormVoClass == null) {
            @Suppress("UNCHECKED_CAST")
            updateFormVoClass = GenericKit.getSuperClassGenricClass(this::class, 7) as KClass<UF>
        }
        return TeminalConstraintsCreator.create(requireNotNull(updateFormVoClass) { "updateFormVoClass is null" })
    }

    /**
     * 返回指定主键的编辑记录
     *
     * @param id 主键
     * @return 编辑VO
     */
    @GetMapping("/getEdit")
    open fun getEdit(id: PK): E {
        if (editVoClass == null) {
            @Suppress("UNCHECKED_CAST")
            editVoClass = GenericKit.getSuperClassGenricClass(this::class, 5) as KClass<E>
        }
        return service.get(id, requireNotNull(editVoClass) { "editVoClass is null" }) ?: throw ObjectNotFoundException("找不到记录！")
    }

    /**
     * 保存新增的记录
     *
     * @param formCreateVo 表单新增VO
     * @return 主键
     */
    @PostMapping("/save")
    open fun save(@RequestBody @Valid formCreateVo: CF): PK {
        return service.insert(formCreateVo)
    }

    /**
     * 更新记录
     *
     * @param formUpdateVo 表单更新VO
     */
    @PutMapping("/update")
    open fun update(@RequestBody @Valid formUpdateVo: UF) {
        service.update(formUpdateVo)
    }

    /**
     * 删除指定主键的记录
     *
     * @param id 主键
     * @return 是否删除成功
     */
    @DeleteMapping("/delete")
    open fun delete(id: PK): Boolean {
        return service.deleteById(id)
    }

    /**
     * 批量删除指定主键的记录
     *
     * @param ids 主键列表
     * @return 是否删除成功
     */
    @PostMapping("/batchDelete")
    open fun batchDelete(@RequestBody ids: List<PK>): Boolean {
        return service.batchDelete(ids) == ids.size
    }

}