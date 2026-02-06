package io.kudos.ability.web.springmvc.controller

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.base.support.payload.FormPayload
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.result.IJsonResult
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


/**
 * 基础的增删改查Controller
 *
 * @param PK 主键类型
 * @param B 业务处理类
 * @param S 列表查询载体类
 * @param R 记录类型
 * @param F 表单实体类
 * @author K
 * @since 1.0.0
 */
open class BaseCrudController<PK : Any, B : IBaseCrudService<PK, *>, S : ListSearchPayload, R : IJsonResult, D : IJsonResult, F : FormPayload<PK>> :
    BaseReadOnlyController<PK, B, S, R, D, F>() {

    /**
     * 保存或更新记录
     *
     * @param payload 表单实体
     * @return WebResult(主键)
     * @author K
     * @since 1.0.0
     */
    @PostMapping("/saveOrUpdate")
    open fun saveOrUpdate(@RequestBody @Valid payload: F): PK {
        return if (payload.id == null || payload.id == "") {
            biz.insert(payload)
        } else {
            biz.update(payload)
            payload.id!!
        }
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
        return biz.deleteById(id)
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
        return biz.batchDelete(ids) == ids.size
    }

}