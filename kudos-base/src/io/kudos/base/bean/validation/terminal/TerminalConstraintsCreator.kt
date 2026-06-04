package io.kudos.base.bean.validation.terminal


import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertorFactory
import io.kudos.base.lang.SystemKit
import io.kudos.base.lang.reflect.getMemberProperty
import io.kudos.base.lang.reflect.getSuperClass
import io.kudos.base.lang.reflect.getSuperInterfaces
import jakarta.validation.Constraint
import jakarta.validation.Valid
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

/**
 * Terminal constraints creator.
 *
 * @author K
 * @since 1.0.0
 */
object TerminalConstraintsCreator {

    /**
     * Map(beanClassName-propertyPrefix, Map(propertyName, LinkedHashMap(constraintName, Array(Map(annotationPropertyName, annotationPropertyValue)))))
     *
     * Request-level validation hot-path cache ([create] is called for Controller / Service argument
     * validation), read and written concurrently. Backed by [ConcurrentHashMap] for lock-free reads and
     * thread-safe writes; [create] uses `computeIfAbsent` to avoid redundant recomputation.
     */
    private val constrainCacheMap =
        ConcurrentHashMap<String, Map<String, LinkedHashMap<String, Array<Map<String, Any>>>>>()

    /**
     * Generates the terminal validation rules for the given bean class.
     *
     * @param beanClass the bean class to be validated
     * @param propertyPrefix the property-name prefix
     * @return Map(propertyName, LinkedHashMap(constraintName, Array(Map(annotationPropertyName, annotationPropertyValue))))
     * @author K
     * @since 1.0.0
     */
    fun create(
        beanClass: KClass<*>,
        propertyPrefix: String = ""
    ): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        val cacheKey = "${beanClass.qualifiedName}-$propertyPrefix"
        // Debug 模式跳缓存（保留原行为：调试期总走重算，便于改注解后立即生效）
        if (SystemKit.isDebug()) {
            val annotations = mutableMapOf<String, MutableList<Annotation>>()
            parseAnnotations(annotations, beanClass, null)
            return genRule(annotations, propertyPrefix, beanClass).also { constrainCacheMap[cacheKey] = it }
        }
        return constrainCacheMap.computeIfAbsent(cacheKey) {
            val annotations = mutableMapOf<String, MutableList<Annotation>>()
            parseAnnotations(annotations, beanClass, null)
            genRule(annotations, propertyPrefix, beanClass)
        }
    }

    /**
     * Parses annotations.
     *
     * @param annotations MutableMap<propertyName, MutableList<annotation>>
     * @param beanClass the bean class to be validated
     * @param parentProperty the parent property
     * @author K
     * @since 1.0.0
     */
    private fun parseAnnotations(
        annotations: MutableMap<String, MutableList<Annotation>>, beanClass: KClass<*>, parentProperty: KProperty<*>?
    ) {
        var clazz = beanClass
        var parentProp = parentProperty
        annotations.putAll(getAnnotationsOnClass(clazz))

        for (prop in clazz.memberProperties) {
            if (prop.returnType != Any::class.starProjectedType) {
                if (prop.getter.hasAnnotation<Valid>()) { // cascaded validation
                    parentProp = prop
                    val parentClazz = clazz
                    clazz = prop.returnType.classifier as? KClass<*>
                        ?: error("Unable to resolve cascaded property type: ${prop.name}")
                    if (clazz == Array<Any>::class) {
                        clazz = requireNotNull(clazz.companionObject) {
                            "Unable to get companionObject for array type: ${clazz.qualifiedName}"
                        }
                    }
                    if (clazz.isSubclassOf(List::class)) {
                        val paramType = prop.typeParameters
                        clazz = paramType[0].starProjectedType.classifier as? KClass<*>
                            ?: error("Unable to resolve generic type of List cascaded property: ${prop.name}")
                    }
                    if (clazz.isSubclassOf(Map::class)) {
                        val paramType = prop.typeParameters
                        clazz = paramType[1].starProjectedType.classifier as? KClass<*>
                            ?: error("Unable to resolve value generic type of Map cascaded property: ${prop.name}")
                    }
                    parseAnnotations(annotations, clazz, parentProp) // recursively parse annotations in all classes
                    clazz = parentClazz
                    parentProp = null
                } else {
                    val propName = parentProp?.let { "'${it.name}.${prop.name}'" } ?: prop.name
                    check(propName.count { it == '.' } <= 1) { "Property nesting exceeds 1 level: $propName" }
                    val annoList = getAnnotationsOnGetter(clazz, prop.name)
                    annotations[propName]?.let(annoList::addAll)
                    if (annoList.isNotEmpty()) {
                        annotations[propName] = annoList
                    }
                }
            }
        }
    }

    /**
     * Builds validation rules, merged by property, and converts the result to JSON.
     *
     * @param annotationsMap MutableMap<propertyName, MutableList<annotation>>
     * @param propertyPrefix property-name prefix
     * @param beanClass the bean class to be validated
     * @return JSON text of validation rules
     * @author K
     * @since 1.0.0
     */
    private fun genRule(
        annotationsMap: Map<String, MutableList<Annotation>>,
        propertyPrefix: String,
        beanClass: KClass<*>
    ): Map<String, LinkedHashMap<String, Array<Map<String, Any>>>> {
        if (annotationsMap.isEmpty()) {
            return emptyMap()
        }
        val ruleMap = mutableMapOf<String, LinkedHashMap<String, Array<Map<String, Any>>>>()
        for ((originalProperty, annotations) in annotationsMap) {
            val property = PropertyResolver.toPotQuote(originalProperty, propertyPrefix)
            val context = ConstraintConvertContext(originalProperty, property, propertyPrefix, beanClass)
            annotations.forEach { annotation ->
                // null entries don't need to be returned to the terminal
                ConstraintConvertorFactory.getInstance(annotation)?.let { converter ->
                    val terminalConstraint = converter.convert(context)
                    ruleMap.getOrPut(property) { linkedMapOf() }[terminalConstraint.constraint] = terminalConstraint.rule
                }
            }
        }
        return ruleMap
    }

    /**
     * Returns the class-level constraint annotations.
     *
     * @param clazz the bean class
     * @return Map(propertyName, List(constraintAnnotation))
     * @author K
     * @since 1.0.0
     */
    private fun getAnnotationsOnClass(clazz: KClass<*>): Map<String, MutableList<Annotation>> {
        val annotationMap = mutableMapOf<String, MutableList<Annotation>>()
        for (annotation in clazz.annotations) {
            // In custom class-level constraint annotations, the property representing the array of class properties is consistently named `properties`
            val prop = annotation::class.getMemberProperty("properties")
            val propertyNames = (prop.call(annotation) as? Array<*>)?.filterIsInstance<String>() ?: emptyList()
            val isConstraint = annotation.annotationClass.hasAnnotation<Constraint>()
                || annotation.annotationClass.qualifiedName?.endsWith(".List") == true
            propertyNames.forEach { propertyName ->
                val annoList = annotationMap.getOrPut(propertyName) { mutableListOf() }
                if (isConstraint) annoList.add(annotation)
            }
        }
        return annotationMap
    }

    /**
     * Returns the constraint annotations on the getter.
     *
     * @param clazz the bean class
     * @return Map(propertyName, List(constraintAnnotation))
     * @author K
     * @since 1.0.0
     */
    private fun getAnnotationsOnGetter(clazz: KClass<*>, property: String): MutableList<Annotation> {
        return collectAnnotationsOnGetter(clazz, property, mutableSetOf())
    }

    /**
     * Recursively collects constraint annotations on the getter.
     * When a subclass overrides a property but does not redeclare its constraints, the search continues into parent interfaces and the superclass.
     *
     * The [visited] set guards against diamond duplicates caused by multiple interface inheritance; the recursion stops immediately upon reaching [Any].
     *
     * @param clazz the class currently being scanned
     * @param property the property name
     * @param visited the set of classes already visited (deduplication)
     * @return the list of matched constraint annotations (may be empty)
     * @author K
     * @since 1.0.0
     */
    private fun collectAnnotationsOnGetter(
        clazz: KClass<*>,
        property: String,
        visited: MutableSet<KClass<*>>
    ): MutableList<Annotation> {
        if (clazz == Any::class || !visited.add(clazz)) {
            return mutableListOf()
        }
        val annotationList = mutableListOf<Annotation>()
        try {
            val prop = clazz.declaredMemberProperties.first { it.name == property }
            prop.getter.annotations.forEach { annotation ->
                if (annotation.annotationClass.hasAnnotation<Constraint>()
                    || annotation.annotationClass.qualifiedName?.endsWith(".List") == true
                ) {
                    annotationList.add(annotation)
                }
            }
        } catch (_: NoSuchElementException) {
            // Current class does not declare this property; continue searching upward
        } catch (_: IllegalArgumentException) {
            // Current class does not declare this property; continue searching upward
        }
        clazz.getSuperInterfaces().forEach { superInterface ->
            annotationList.addAll(collectAnnotationsOnGetter(superInterface, property, visited))
        }
        clazz.getSuperClass()?.let { superClass ->
            annotationList.addAll(collectAnnotationsOnGetter(superClass, property, visited))
        }
        return annotationList
    }
}
