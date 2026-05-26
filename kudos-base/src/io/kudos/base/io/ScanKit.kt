package io.kudos.base.io

import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass


/**
 * Class and resource lookup utility.
 *
 * @author K
 * @since 1.0.0
 */
object ScanKit {

    /**
     * Finds classes annotated with the given annotation.
     *
     * @param basePackage the package to scan
     * @param annotationClass the annotation class to look for
     * @param recursive whether to recursively scan subpackages; defaults to true
     * @return the list of classes annotated with the given annotation
     * @author K
     * @since 1.0.0
     */
    fun <T : Annotation> findClassesWithAnnotation(
        basePackage: String,
        annotationClass: KClass<T>,
        recursive: Boolean = true
    ): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo().enableAnnotationInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getClassesWithAnnotation(annotationClass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * Finds subclasses of the given superclass.
     *
     * @param basePackage the package to scan
     * @param superclass the superclass whose subclasses are sought; must not be an interface (use `findImplementations`
     *                    for interfaces)
     * @param recursive whether to recursively scan subpackages; defaults to true
     * @return the list of subclasses of the given superclass
     * @author K
     * @since 1.0.0
     */
    fun findSubclassesOf(basePackage: String, superclass: KClass<*>, recursive: Boolean = true): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getSubclasses(superclass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * Finds classes that implement the given interface.
     *
     * @param basePackage the package to scan
     * @param interfaceClass the interface whose implementations are sought; must not be a class (use `findSubclassesOf`
     *                       for classes)
     * @param recursive whether to recursively scan subpackages; defaults to true
     * @return the list of classes implementing the given interface
     * @author K
     * @since 1.0.0
     */
    fun findImplementations(
        basePackage: String,
        interfaceClass: KClass<*>,
        recursive: Boolean = true
    ): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getClassesImplementing(interfaceClass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * Finds classes whose simple name matches the given pattern.
     *
     * @param basePackage the package to scan
     * @param pattern the class-name pattern to match (regular expression)
     * @param recursive whether to recursively scan subpackages; defaults to true
     * @return the list of classes whose name matches the given pattern
     * @author K
     * @since 1.0.0
     */
    fun findClassesMatching(basePackage: String, pattern: String, recursive: Boolean = true): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        val regexPattern = pattern.toRegex()
        return classGraph.scan()
            .use { scanResult ->
                scanResult.allClasses
                    .filter { it.simpleName.matches(regexPattern) }
                    .loadClasses().map { it.kotlin }
            }
    }

    /**
     * Finds resources whose filename matches the given pattern.
     *
     * @param basePackage the package to scan
     * @param pattern the resource filename pattern to match (regular expression)
     * @param recursive whether to recursively scan subpackages; defaults to true
     * @return the list of paths of resources whose filename matches the given pattern
     * @author K
     * @since 1.0.0
     */
    fun findResourcesMatching(basePackage: String, pattern: String, recursive: Boolean = true): List<String> {
        var classGraph = ClassGraph()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        val regexPattern = pattern.toRegex()
        return classGraph.scan()
            .use { scanResult ->
                scanResult.allResources
                    .filter { it.path.matches(regexPattern) }
                    .map { it.path }
            }
    }

}
