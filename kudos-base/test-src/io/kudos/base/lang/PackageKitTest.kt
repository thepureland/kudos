package io.kudos.base.lang

import io.kudos.base.lang.string.EncodeKit
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import kotlin.test.Test

/**
 * PackageKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class PackageKitTest {

    @Test
    fun testGetClassesInPackage() {
        // in file
        val packageName = PackageKit::class.java.getPackage().name
        var classes = PackageKit.getClassesInPackage(packageName, true)
        assert(classes.contains(PackageKit::class))
        assert(classes.contains(EncodeKit::class))
        assert(classes.contains(SystemKit::class))

        // in jar
        classes = PackageKit.getClassesInPackage("org.apache.commons.lang3", true)
        assert(classes.contains(StringUtils::class))
        assert(classes.contains(BooleanUtils::class))
    }

    @Test
    fun testGetPackages() {
        // in file
        var packages = PackageKit.getPackages("io.kudos.base.*", true)
        assert(packages.contains("io.kudos.base.lang"))
        assert(packages.contains("io.kudos.base.logger"))

        // in jar using package pattern
        packages = PackageKit.getPackages("org.apache.**.lang3", true)
        assert(packages.contains("org.apache.commons.lang3"))
    }
    
}