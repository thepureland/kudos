plugins {
    // 启用 Kotlin Serialization 插件，使编译期能生成序列化器
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(libs.kotlinReflect)
    api(libs.kotlinxCoroutines)

    // bean
    api(libs.apache.commons.beanutils)

    // codec
    api(libs.apache.commons.codec)

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
}


// ./gradlew dependencySizes
tasks.register("dependencySizes") {
    group = "help"
    description = "列出 runtimeClasspath 下所有依赖 Jar 的大小（KB），按大小从大到小排序，并输出格式对齐"
    doLast {
        val config = configurations.getByName("runtimeClasspath")
        val artifacts = config.resolvedConfiguration.resolvedArtifacts

        // 先收集坐标和大小
        val artifactSizes = artifacts.map { art ->
            val sizeKb = art.file.length() / 1024
            val coord = "${art.moduleVersion.id.group}:${art.name}:${art.moduleVersion.id.version}"
            coord to sizeKb
        }

        // 按大小倒序
        val sorted = artifactSizes.sortedByDescending { it.second }

        // 计算列宽（coord 列最大长度，不超过 80）
        val coordWidth = (sorted.maxOfOrNull { it.first.length } ?: 0).coerceAtMost(80)
        val sizeHeader = "大小(KB)"
        val headerCoord = "依赖坐标".padEnd(coordWidth)
        println("$headerCoord    $sizeHeader")
        println("-".repeat(coordWidth + 4 + sizeHeader.length))

        // 输出每条
        var totalKb = 0L
        sorted.forEach { (coord, sizeKb) ->
            totalKb += sizeKb
            println(coord.padEnd(coordWidth) + "    " + sizeKb.toString().padStart(sizeHeader.length))
        }

        // 输出合计
        println("-".repeat(coordWidth + 4 + sizeHeader.length))
        val totalLabel = "总计".padEnd(coordWidth)
        println(totalLabel + "    " + totalKb.toString().padStart(sizeHeader.length) + " KB")
    }
}