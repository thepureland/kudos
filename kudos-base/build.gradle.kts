plugins {
    // 启用 Kotlin Serialization 插件，使编译期能生成序列化器
    kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinxCoroutines}}")

    // bean
    api("commons-beanutils:commons-beanutils:1.11.0")

    // codec
    api("commons-codec:commons-codec:1.18.0")

    // lang3
    api("org.apache.commons:commons-lang3:3.17.0")

    // text
    api("org.apache.commons:commons-text:1.13.1")

    // net
    api("commons-net:commons-net:3.11.1")

    // validation
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    api("org.hibernate.validator:hibernate-validator:9.0.0.Final")
    api("jakarta.el:jakarta.el-api:6.0.1")
    api("org.glassfish:jakarta.el:5.0.0-M1")
//    api("javax.money:money-api:1.1")

    // json
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:${libs.versions.ktor.get()}")

    // log
    api("org.slf4j:jcl-over-slf4j:2.0.17")
    api("org.slf4j:log4j-over-slf4j:2.0.17")
    api("org.slf4j:log4j-over-slf4j:2.0.17")
    api("ch.qos.logback:logback-classic:1.5.18")

    // excel
    api("net.sourceforge.jexcelapi:jxl:2.6.12")

    // xml
    api("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    api("org.glassfish.jaxb:jaxb-runtime:4.0.2")

    // barcode
    api("com.google.zxing:core:3.5.3")

    // zip
    api("net.lingala.zip4j:zip4j:2.11.5")

    // svg
    api("org.apache.xmlgraphics:batik-dom:1.19")
    api("org.apache.xmlgraphics:batik-bridge:1.19")

    // class scan
    api("io.github.classgraph:classgraph:4.8.179")
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