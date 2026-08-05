package io.kudos.ms.sys.api.admin

import java.lang.reflect.Field

/**
 * Reflection helpers for the admin controller unit tests: the controllers receive their
 * collaborators via Spring field injection (`@Autowired` base-class `service` /
 * private `@Resource` fields), so pure unit tests assign Mockito mocks directly to those fields.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal object ControllerTestKit {

    /**
     * Set [value] into the (possibly inherited, possibly private) field named [fieldName]
     * of [target], searching the class hierarchy from the concrete class upwards.
     */
    fun inject(target: Any, fieldName: String, value: Any) {
        val field = findField(target.javaClass, fieldName)
            ?: throw NoSuchFieldException("Field '$fieldName' not found on ${target.javaClass.name} hierarchy")
        field.isAccessible = true
        field.set(target, value)
    }

    /** Inject the base-class `service` field declared in BaseReadOnlyController. */
    fun injectService(controller: Any, service: Any) = inject(controller, "service", service)

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            val field = current.declaredFields.firstOrNull { it.name == name }
            if (field != null) return field
            current = current.superclass
        }
        return null
    }
}
