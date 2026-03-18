package io.kudos.ability.web.springmvc.controller

import io.kudos.base.bean.validation.teminal.TeminalConstraintsCreator
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.support.iservice.IBaseCrudService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import kotlin.reflect.KClass


/**
 * 基础的增删改查Controller
 *
 * @param PK 主键类型
 * @param B 业务处理类
 * @param S 列表查询载体类
 * @param R 记录类型
 * @param CF 新增页表单实体类
 * @param UF 编辑页表单实体类
 * @author K
 * @since 1.0.0
 */
open class BaseCrudController<
        PK: Any,
        B: IBaseCrudService<PK, *>,
        S: ListSearchPayload,
        R: Any,
        D: Any,
        CF: Any,
        UF: Any>
    :BaseReadOnlyController<PK, B, S, R, D>() {

    private var createFormModelClass: KClass<CF>? = null

    private var updateFormModelClass: KClass<UF>? = null

    /**
     * 返回新增页表单校验规则
     *
     * @return WebResult(Map(属性名， LinkedHashMap(约束名，Array(Map(约束注解的属性名，约束注解的属性值)))))
     */
    @GetMapping("/getCreateValidationRule")
    open fun getCreateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        if (createFormModelClass == null) {
            createFormModelClass = getCreateFormModelClass()
        }
        return TeminalConstraintsCreator.create(requireNotNull(createFormModelClass) { "createFormModelClass is null" })
    }

    /**
     * 返回编辑页表单校验规则
     *
     * @return WebResult(Map(属性名， LinkedHashMap(约束名，Array(Map(约束注解的属性名，约束注解的属性值)))))
     */
    @GetMapping("/getUpdateValidationRule")
    open fun getUpdateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        if (updateFormModelClass == null) {
            updateFormModelClass = getUpdateFormModelClass()
        }
        return TeminalConstraintsCreator.create(requireNotNull(updateFormModelClass) { "updateFormModelClass is null" })
    }

    /**
     * 保存新增的记录
     *
     * @param payload 表单实体
     * @return WebResult(主键)
     */
    @PostMapping("/save")
    open fun save(@RequestBody @Valid payload: CF): PK {
        return service.insert(payload)
    }

    /**
     * 更新记录
     *
     * @param payload 表单实体
     * @return WebResult(主键)
     */
    @PutMapping("/update")
    open fun update(@RequestBody @Valid payload: UF) {
        service.update(payload)
    }

    /**
     * 删除指定主键的记录
     *
     * @param id 主键
     * @return WebResult(是否删除成功)
     * @author K
     * @since 1.0.0
     */
    @DeleteMapping("/delete")
    open fun delete(id: PK): Boolean {
        return service.deleteById(id)
    }

    /**
     * 批量删除指定主键的记录
     *
     * @param ids 主键列表
     * @return WebResult(是否删除成功)
     * @author K
     * @since 1.0.0
     */
    @PostMapping("/batchDelete")
    open fun batchDelete(@RequestBody ids: List<PK>): Boolean {
        return service.batchDelete(ids) == ids.size
    }

    /**
     * 返回新增页表单模型类
     *
     * @param CF 表单模型类
     * @return KClass
     */
    @Suppress("UNCHECKED_CAST")
    open fun getCreateFormModelClass(): KClass<CF> {
        return GenericKit.getSuperClassGenricClass(this::class, 5) as KClass<CF>
    }

    /**
     * 返回编辑页表单模型类
     *
     * @param UF 表单模型类
     * @return KClass
     */
    @Suppress("UNCHECKED_CAST")
    open fun getUpdateFormModelClass(): KClass<UF> {
        return GenericKit.getSuperClassGenricClass(this::class, 6) as KClass<UF>
    }

}