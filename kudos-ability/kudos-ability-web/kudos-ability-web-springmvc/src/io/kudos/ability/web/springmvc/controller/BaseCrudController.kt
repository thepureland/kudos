package io.kudos.ability.web.springmvc.controller

import io.kudos.base.bean.validation.terminal.TerminalConstraintsCreator
import io.kudos.base.error.ObjectNotFoundException
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
 * The VO classes are inferred from the subclass's type arguments by default, which requires the subclass to
 * extend this class **directly**; pass them to the constructor to lift that restriction. See
 * [BaseReadOnlyController] for the full explanation.
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
 * @author AI: Claude
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
        UF: Any>(
    /** Detail VO class; when null it is inferred from the subclass's type arguments. */
    explicitDetailVoClass: KClass<D>? = null,
    /** Edit VO class; when null it is inferred from the subclass's type arguments. */
    explicitEditVoClass: KClass<E>? = null,
    /** Create-form VO class; when null it is inferred from the subclass's type arguments. */
    explicitCreateFormVoClass: KClass<CF>? = null,
    /** Update-form VO class; when null it is inferred from the subclass's type arguments. */
    explicitUpdateFormVoClass: KClass<UF>? = null
) : BaseReadOnlyController<PK, B, S, R, D>(explicitDetailVoClass) {

    /** Edit VO class: the constructor-supplied one, else resolved lazily from the subclass's type arguments. */
    protected val editVoClass: KClass<E> by lazy {
        explicitEditVoClass ?: resolveTypeArgument(TYPE_ARG_EDIT_VO, "E (edit VO)")
    }

    /** Create-form VO class: the constructor-supplied one, else resolved lazily from the subclass's type arguments. */
    protected val createFormVoClass: KClass<CF> by lazy {
        explicitCreateFormVoClass ?: resolveTypeArgument(TYPE_ARG_CREATE_FORM_VO, "CF (create-form VO)")
    }

    /** Update-form VO class: the constructor-supplied one, else resolved lazily from the subclass's type arguments. */
    protected val updateFormVoClass: KClass<UF> by lazy {
        explicitUpdateFormVoClass ?: resolveTypeArgument(TYPE_ARG_UPDATE_FORM_VO, "UF (update-form VO)")
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

    companion object {

        /** Position of `E` (edit VO) in this class's type parameter list. */
        const val TYPE_ARG_EDIT_VO = 5

        /** Position of `CF` (create-form VO) in this class's type parameter list. */
        const val TYPE_ARG_CREATE_FORM_VO = 6

        /** Position of `UF` (update-form VO) in this class's type parameter list. */
        const val TYPE_ARG_UPDATE_FORM_VO = 7

    }

}
