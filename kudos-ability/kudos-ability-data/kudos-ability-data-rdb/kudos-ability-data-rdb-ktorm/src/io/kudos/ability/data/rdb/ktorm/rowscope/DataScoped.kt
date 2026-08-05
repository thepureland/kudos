package io.kudos.ability.data.rdb.ktorm.rowscope


/**
 * Declares that this entity's rows are sliced by [dimension], and which column carries that
 * dimension's value.
 *
 * ```kotlin
 * @DataScoped(dimension = "org", column = "org_id")
 * @DataScoped(dimension = "region", column = "region_code")
 * interface BizOrder : IDbEntity<String, BizOrder> { … }
 * ```
 *
 * **The declaration lives on the entity, not in a central config file.** A scope rule and the table
 * shape it depends on have to change together; keeping them apart guarantees they drift, and a rule
 * that points at a column somebody renamed fails open.
 *
 * **Entities are opt-in.** An entity with no declaration is not filtered — the opposite of the
 * unregistered-URL rule, and deliberately so: dictionaries, configuration and the authorization
 * tables themselves are genuinely global, so default-filtering would be the wrong answer for most
 * tables and would lock the permission admin screens out of their own data first. The cost —
 * "forgot to declare" means "no protection" — is covered by the startup report, which lists every
 * undeclared entity so the gap is visible rather than silent.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
@MustBeDocumented
annotation class DataScoped(

    /** The scope dimension, matching `auth_role_scope.dimension` (`org`, `region`, …). */
    val dimension: String,

    /** The database column on this entity that carries the dimension's value. */
    val column: String,
)


/**
 * Declares which column identifies the row's creator, for the SELF scope policy ("only rows I
 * created").
 *
 * Separate from [DataScoped] because it is not a dimension: SELF is not a set of values a role is
 * granted, it is a comparison against whoever is asking.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DataScopedSelf(

    /** The column holding the creator's principal id, e.g. `create_user_id`. */
    val column: String,
)


/**
 * Marks a method as running with system authority: row filtering does not apply inside it.
 *
 * Needed by scheduled jobs, message consumers and service-to-service calls, which legitimately have
 * no logged-in subject. See [DataScopeContext] for why this must be explicit rather than inferred
 * from the absence of a subject.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SystemScoped
