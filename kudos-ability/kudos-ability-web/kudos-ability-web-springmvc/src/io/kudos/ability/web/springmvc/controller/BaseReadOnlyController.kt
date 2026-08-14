package io.kudos.ability.web.springmvc.controller

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.reflect.KClass


/**
 * Base read-only Controller.
 *
 * ## Resolving the VO types
 *
 * The response VO classes are needed at runtime, and by default they are read back off the subclass's type
 * arguments by reflection. [GenericKit.getSuperClassGenricClass] reads the arguments bound on the direct
 * supertype, walking further up when that supertype binds none of its own. So:
 *
 * - extending this class **directly** — works;
 * - extending through a **non-generic** intermediate that binds every argument itself — also works, because the
 *   walk-up finds those bindings;
 * - extending through a **generic** intermediate that leaves arguments open — **does not work**: the leaf's
 *   direct supertype then carries the intermediate's type parameters, and the positions no longer line up with
 *   this class's declaration.
 *
 * For that last case, pass the classes to the constructor instead:
 *
 * ```kotlin
 * abstract class TenantScopedController<D : Any>(detail: KClass<D>) :
 *     BaseReadOnlyController<Long, SomeService, SomeQuery, SomeRow, D>(detail)
 *
 * class UserController : TenantScopedController<UserDetail>(UserDetail::class)
 * ```
 *
 * Reflection then never runs, and the generic intermediate is fine.
 *
 * @param PK primary key type
 * @param B business service class
 * @param S list query condition VO class (request)
 * @param R list row VO class (response)
 * @param D detail VO class (response)
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class BaseReadOnlyController<
        PK : Any,
        B : IBaseReadOnlyService<PK, *>,
        S : ListSearchPayload,
        R : Any,
        D: Any>(
    /** Detail VO class; when null it is inferred from the subclass's type arguments. */
    explicitDetailVoClass: KClass<D>? = null
) : BaseController() {

    @Autowired
    protected lateinit var service: B

    /** Detail VO class: the constructor-supplied one, else resolved lazily from the subclass's type arguments. */
    protected val detailVoClass: KClass<D> by lazy {
        explicitDetailVoClass ?: resolveTypeArgument(TYPE_ARG_DETAIL_VO, "D (detail VO)")
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

    /**
     * Read one of this controller's type arguments reflectively.
     *
     * Failure is reported with the constraint spelled out and the way around it named. The underlying reflection
     * reports an out-of-bounds index or hands back `Any`/`Nothing`, none of which tells the reader that the real
     * problem is an intermediate class in the hierarchy — a diagnosis that otherwise costs a debugging session
     * to reach.
     *
     * @param T the expected VO type
     * @param index position of the type parameter in this class's declaration
     * @param description human-readable name of the parameter, used in the failure message
     * @return the bound type argument
     * @throws IllegalStateException when the argument cannot be determined
     * @author K
     * @since 1.0.0
     */
    protected fun <T : Any> resolveTypeArgument(index: Int, description: String): KClass<T> {
        val resolved = try {
            GenericKit.getSuperClassGenricClass(this::class, index)
        } catch (e: Exception) {
            throw IllegalStateException(unresolvableTypeArgumentMessage(description), e)
        }
        check(resolved != Any::class && resolved != Nothing::class) {
            unresolvableTypeArgumentMessage(description)
        }
        @Suppress("UNCHECKED_CAST")
        return resolved as KClass<T>
    }

    /**
     * Build the failure message for an unresolvable type argument.
     *
     * @param description human-readable name of the parameter
     * @return the message
     * @author K
     * @since 1.0.0
     */
    private fun unresolvableTypeArgumentMessage(description: String): String =
        "Unable to resolve the type argument $description of ${this::class.qualifiedName}. " +
            "Type arguments are read off the direct superclass, so this only works when the controller extends " +
            "${BaseReadOnlyController::class.simpleName}/${BaseCrudController::class.simpleName} directly. " +
            "If an intermediate base class is required, pass the VO classes to the constructor instead."

    companion object {

        /** Position of `D` (detail VO) in this class's type parameter list. */
        const val TYPE_ARG_DETAIL_VO = 4

    }

}
