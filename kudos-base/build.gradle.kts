dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinxCoroutines}}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${libs.versions.kotlinxCoroutines}") {}

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

    // json
    api("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    api("com.alibaba.fastjson2:fastjson2:2.0.57")

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
    api("de.idyl:winzipaes:1.0.1")

    // svg
    api("org.apache.xmlgraphics:batik-dom:1.19")
    api("org.apache.xmlgraphics:batik-bridge:1.19")

    // class scan
    api("io.github.classgraph:classgraph:4.8.179")
}