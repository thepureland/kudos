object Version {
    const val KOTLIN = "1.8.10"
    const val SOUL = "5.0.0.0-SNAPSHOT"
    const val KTORM = "3.6.0"
}

plugins {
    kotlin("jvm") version "1.8.10"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("org.openjfx.javafxplugin") version "0.0.8"
}

/* 插件的配置需要在buildscript元素中 */
buildscript {
    /* 插件仓库 */
//    repositories {
//        maven { url = uri("https://plugins.gradle.org/m2/") }
//    }
    /* 插件依赖 */
//    dependencies {
//        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.4.6")
//        classpath("io.spring.gradle:dependency-management-plugin:1.0.9.RELEASE")
//    }
}

allprojects {
    group = "io.kudos"
    version = "1.0.0-SNAPSHOT"
    buildDir = file("${rootProject.projectDir}/build/${project.name}")

    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
//        mavenCentral()
////        google() // 大陆访问不了
//        maven { url = uri("https://repo1.maven.org/maven2") }
////        maven { url = uri("https://mvnrepository.com") } // 会导致下载maven-metadata.xml时卡很久
//        maven { url = uri("https://central.sonatype.com") }
        maven {
            isAllowInsecureProtocol = true
            url = uri("https://nexus.soulworld.net/#browse/browse:maven-public")
        }
        mavenLocal()
    }

    apply {
        plugin("kotlin")
        plugin("idea")
        plugin("io.spring.dependency-management")
        plugin("org.openjfx.javafxplugin")
    }

    // 为了不让父工程生成src、resources、test、testresources四个目录
    sourceSets {
        main {
            kotlin {
                setSrcDirs(emptyList<String>())
            }
            java {
                setSrcDirs(emptyList<String>())
            }
            resources {
                setSrcDirs(emptyList<String>())
            }
        }
        test {
            kotlin {
                setSrcDirs(emptyList<String>())
            }
            java {
                setSrcDirs(emptyList<String>())
            }
            resources {
                setSrcDirs(emptyList<String>())
            }
        }
    }

}

subprojects {

    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    ext {
    }

    configurations {
        // 所有需要忽略的包定义在此
//        all*.exclude group: 'commons-httpclient'
//        all*.exclude group: 'commons-beanutils', module: 'commons-beanutils'
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

//    tasks.withType<BootJar> {
//        enabled = false
//    }

    dependencies {
        // kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect:${Version.KOTLIN}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
        testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.4")
    }

    dependencyManagement {
//        resolutionStrategy {
//            cacheChangingModulesFor(0, "seconds")
//            cacheDynamicVersionsFor(0, "seconds")
//        }

        imports {
//            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:${Version.ALIBABA_CLOUD}")
        }
        dependencies {
            // soul
            dependency("org.soul:soul-base:${Version.SOUL}")
            dependency("org.soul:soul-context:${Version.SOUL}")
            dependency("org.soul:soul-ability-data-rdb-jdbc:${Version.SOUL}")
            dependency("org.soul:soul-ability-data-rdb-flyway:${Version.SOUL}")
            dependency("org.soul:soul-ability-data-memdb-redis:${Version.SOUL}")
            dependency("org.soul:soul-ability-data-docdb-mongo:${Version.SOUL}")
            dependency("org.soul:soul-ability-cache-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-cache-local-caffeine:${Version.SOUL}")
            dependency("org.soul:soul-ability-cache-remote-redis:${Version.SOUL}")
            dependency("org.soul:soul-ability-cache-interservice:${Version.SOUL}")
            dependency("org.soul:soul-ability-web-springmvc:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-netty:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-websocket:${Version.SOUL}")
            dependency("org.soul:soul-ability-captcha-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-captcha-common-impl:${Version.SOUL}")
            dependency("org.soul:soul-ability-captcha-tianai:${Version.SOUL}")
            dependency("org.soul:soul-ability-captcha-web:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-client:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-config:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-discovery:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-lock:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-tx:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-client:${Version.SOUL}")
            dependency("org.soul:soul-ability-web-swagger:${Version.SOUL}")
            dependency("org.soul:soul-ability-file-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-file-local:${Version.SOUL}")
            dependency("org.soul:soul-ability-file-minio:${Version.SOUL}")
            dependency("org.soul:soul-ability-file-oss:${Version.SOUL}")
            dependency("org.soul:soul-ability-engine-workflow:${Version.SOUL}")
            dependency("org.soul:soul-ability-log-audit-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-log-audit-mongo:${Version.SOUL}")
            dependency("org.soul:soul-ability-log-audit-mq:${Version.SOUL}")
            dependency("org.soul:soul-ability-log-audit-rdb:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-email:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-sms-aliyun:${Version.SOUL}")
            dependency("org.soul:soul-ability-comm-sms-aws:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-notify-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-notify-mq:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-notify-rdb:${Version.SOUL}")
            dependency("org.soul:soul-ability-security-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-security-core:${Version.SOUL}")
            dependency("org.soul:soul-ability-security-spring:${Version.SOUL}")
            dependency("org.soul:soul-ability-security-web:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-stream-common:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-stream-kafka:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-stream-rabbit:${Version.SOUL}")
            dependency("org.soul:soul-ability-distributed-stream-rocketmq:${Version.SOUL}")
            dependency("org.soul:soul-ms-captcha-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-captcha-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-captcha-client:${Version.SOUL}")
            dependency("org.soul:soul-ms-captcha-common:${Version.SOUL}")
            dependency("org.soul:soul-ms-captcha-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-fserver-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-log-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-log-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-log-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-notice-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-notice-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-notice-client:${Version.SOUL}")
            dependency("org.soul:soul-ms-notice-common:${Version.SOUL}")
            dependency("org.soul:soul-ms-notice-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-sys-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-sys-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-sys-client:${Version.SOUL}")
            dependency("org.soul:soul-ms-sys-common:${Version.SOUL}")
            dependency("org.soul:soul-ms-sys-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-user-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-user-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-user-client:${Version.SOUL}")
            dependency("org.soul:soul-ms-user-common:${Version.SOUL}")
            dependency("org.soul:soul-ms-user-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-workflow-api-service:${Version.SOUL}")
            dependency("org.soul:soul-ms-workflow-api-view:${Version.SOUL}")
            dependency("org.soul:soul-ms-workflow-client:${Version.SOUL}")
            dependency("org.soul:soul-ms-workflow-common:${Version.SOUL}")
            dependency("org.soul:soul-ms-workflow-service:${Version.SOUL}")

            // base
            dependency("io.github.classgraph:classgraph:4.8.157")
//            dependency("com.alibaba:fastjson:2.0.26")

            // data
            dependency("org.ktorm:ktorm-core:${Version.KTORM}")
            dependency("org.ktorm:ktorm-jackson:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-mysql:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-postgresql:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-oracle:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-sqlserver:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-sqlite:${Version.KTORM}")

//            // ktor
//            dependency("io.ktor:ktor-server-sessions:${Version.KTOR}")
//            dependency("io.ktor:ktor-server-netty:${Version.KTOR}")
////            dependency("io.ktor:ktor-server-tomcat:${DependencyVersions.KTOR_VERSION}")
//            dependency("io.ktor:ktor-server-core:${Version.KTOR}")
//            dependency("io.ktor:ktor-html-builder:${Version.KTOR}")
////            dependency("org.jetbrains:kotlin-css-jvm:1.0.0-pre.31-kotlin-1.2.41")
//            dependency("io.ktor:ktor-client-core:${Version.KTOR}")
//            dependency("io.ktor:ktor-client-core-jvm:${Version.KTOR}")
//            dependency("io.ktor:ktor-client-apache:${Version.KTOR}")
//            dependency("io.ktor:ktor-server-tests:${Version.KTOR}")

            // javafx
//            dependency("de.roskenet:springboot-javafx-support:2.1.6")
//            dependency("org.openjfx:javafx-controls:12.0.1")
            dependency("org.controlsfx:controlsfx:8.40.10")

            // tools
            dependency("org.freemarker:freemarker:2.3.30")


            // test
            dependency("org.springframework.boot:spring-boot-starter-aop:3.0.4")
            dependency("org.springframework.boot:spring-boot-starter-test:3.0.4")
            dependency("com.h2database:h2:2.1.214")
            dependency("org.postgresql:postgresql:42.2.20")
            dependency("org.ktorm:ktorm-support-postgresql:${Version.KTORM}")

        }
    }

}