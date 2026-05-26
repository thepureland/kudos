plugins {
    // Enable the Kotlin Serialization plugin so serializers can be generated at compile time.
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(libs.kotlinReflect)
    api(libs.kotlinxCoroutines)

    // bean
    api(libs.apache.commons.beanutils)

    // codec
    api(libs.apache.commons.codec)

    // BCrypt password hashing (used by PasswordKit); only pull in the spring-security-crypto jar, not the full spring-security stack.
    api(platform(libs.spring.boot.bom))
    api(libs.spring.security.crypto)

    // lang3
    api(libs.apache.commons.lang3)

    // text
    api(libs.apache.commons.text)

    // net
    api(libs.apache.commons.net)

    // validation
    api(libs.jakarta.validation.api)
    api(libs.hibernate.validator)
    api(libs.jakarta.el.api)
    api(libs.glassfish.jakarta.el)
//    api("javax.money:money-api:1.1")

    // json
    api(libs.ktor.serialization.json)

    // log
    api(libs.jcl.over.slf4j)
    api(libs.log4j.over.slf4j)
    api(libs.logback.classic)

    // excel
    api(libs.sourceforge.jxl)

    // xml
    api(libs.jakarta.xml.bind.api)
    api(libs.glassfish.jaxb.runtime)

    // barcode
    api(libs.google.zxing.core)

    // zip
    api(libs.lingala.zip4j)

    // svg
    api(libs.apache.xmlgraphics.batik.dom)
    api(libs.apache.xmlgraphics.batik.bridge)

    // class scan
    api(libs.github.classgraph)

    testImplementation(libs.kotlin.test.junit5)
}


// ./gradlew dependencySizes
tasks.register("dependencySizes") {
    group = "help"
    description = "List the size (KB) of every dependency Jar on the runtimeClasspath, sorted from largest to smallest with aligned formatting."
    doLast {
        val config = configurations.getByName("runtimeClasspath")
        val artifacts = config.resolvedConfiguration.resolvedArtifacts

        // Collect coordinates and sizes first.
        val artifactSizes = artifacts.map { art ->
            val sizeKb = art.file.length() / 1024
            val coord = "${art.moduleVersion.id.group}:${art.name}:${art.moduleVersion.id.version}"
            coord to sizeKb
        }

        // Sort by size in descending order.
        val sorted = artifactSizes.sortedByDescending { it.second }

        // Compute column width (max length of coord column, capped at 80).
        val coordWidth = (sorted.maxOfOrNull { it.first.length } ?: 0).coerceAtMost(80)
        val sizeHeader = "Size(KB)"
        val headerCoord = "Dependency Coordinate".padEnd(coordWidth)
        println("$headerCoord    $sizeHeader")
        println("-".repeat(coordWidth + 4 + sizeHeader.length))

        // Print each row.
        var totalKb = 0L
        sorted.forEach { (coord, sizeKb) ->
            totalKb += sizeKb
            println(coord.padEnd(coordWidth) + "    " + sizeKb.toString().padStart(sizeHeader.length))
        }

        // Print the total.
        println("-".repeat(coordWidth + 4 + sizeHeader.length))
        val totalLabel = "Total".padEnd(coordWidth)
        println(totalLabel + "    " + totalKb.toString().padStart(sizeHeader.length) + " KB")
    }
}