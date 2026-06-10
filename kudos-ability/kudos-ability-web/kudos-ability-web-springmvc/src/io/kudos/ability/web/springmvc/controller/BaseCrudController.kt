package io.kudos.ability.web.springmvc.controller

import io.kudos.base.bean.validation.terminal.TerminalConstraintsCreator
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.support.service.iservice.IBaseCrudService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.LinkedHashMap
import kotlin.reflect.KClass


/**
 * Base CRUD Controller.
 *
 * @param PK primary key type
 * @param B business service class
 * @param S list query condition VO class (request)
 * @param R list query result VO class (response)
 * @param D detail VO class (response)
 * @param E edit VO class (response)
 * @param CF create-form VO class (request)
 * @param UF update-form VO class (request)
 * @author K
 * @author AI: Codex
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

    /** Create-form VO class, resolved lazily from the subclass's generic parameter (index 6). */
    @Suppress("UNCHECKED_CAST")
    private val createFormVoClass: KClass<CF> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 6) as KClass<CF>
    }

    /** Update-form VO class, resolved lazily from the subclass's generic parameter (index 7). */
    @Suppress("UNCHECKED_CAST")
    private val updateFormVoClass: KClass<UF> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 7) as KClass<UF>
    }

    /** Edit VO class, resolved lazily from the subclass's generic parameter (index 5). */
    @Suppress("UNCHECKED_CAST")
    private val editVoClass: KClass<E> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 5) as KClass<E>
    }

    /**
     * Return the validation rules for the create form.
     *
     * @return WebResult(Map(property name, LinkedHashMap(constraint name, Array(Map(constraint annotation attribute name, constraint annotation attribute value)))))
     */
    @GetMapping("/getCreateValidationRule")
    open fun getCreateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> =
        TerminalConstraintsCreator.create(createFormVoClass)

    /**
     * Return the validation rules for the edit form.
     *
     * @return WebResult(Map(property name, LinkedHashMap(constraint name, Array(Map(constraint annotation attribute name, constraint annotation attribute value)))))
     */
    @GetMapping("/getUpdateValidationRule")
    open fun getUpdateValidationRule(): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> =
        TerminalConstraintsCreator.create(updateFormVoClass)

    /**
     * Return the edit record for the given primary key.
     *
     * @param id primary key
     * @return edit VO
     */
    @GetMapping("/getEdit")
    open fun getEdit(id: PK): E =
        service.get(id, editVoClass) ?: throw ObjectNotFoundException("Record not found!")

    /**
     * Save the newly created record.
     *
     * @param formCreateVo create-form VO
     * @return primary key
     */
    @PostMapping("/save")
    open fun save(@RequestBody @Valid formCreateVo: CF): PK = service.insert(formCreateVo)

    /**
     * Update a record.
     *
     * @param formUpdateVo update-form VO
     */
    @PutMapping("/update")
    open fun update(@RequestBody @Valid formUpdateVo: UF) {
        service.update(formUpdateVo)
    }

    /**
     * Delete the record with the given primary key.
     *
     * @param id primary key
     * @return whether deletion succeeded
     */
    @DeleteMapping("/delete")
    open fun delete(id: PK): Boolean = service.deleteById(id)

    /**
     * Batch delete records with the given primary keys.
     *
     * @param ids list of primary keys
     * @return whether deletion succeeded
     */
    @PostMapping("/batchDelete")
    open fun batchDelete(@RequestBody ids: List<PK>): Boolean = service.batchDelete(ids) == ids.size

}