object Version {
    const val KOTLIN = "1.8.10"
    const val SOUL = "5.0.0.0-SNAPSHOT"
    const val KTORM = "3.2.0"
}

plugins {
    kotlin("jvm") version "1.8.10"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
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
        mavenCentral()
//        google() // 大陆访问不了
        maven { url = uri("https://repo1.maven.org/maven2") }
//        maven { url = uri("https://mvnrepository.com") } // 会导致下载maven-metadata.xml时卡很久
        maven { url = uri("https://central.sonatype.com") }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://nexus.soulworld.net/repository/maven-public")
        }
        mavenLocal()
    }

    apply {
        plugin("kotlin")
        plugin("idea")
        plugin("io.spring.dependency-management")
    }

    // 为了不让主工程生成src、resources、test、testresources四个目录
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

    sourceSets {
        main {
            kotlin {
                setSrcDirs(listOf("src"))
            }
            java {
                setSrcDirs(listOf("src"))
            }
            resources {
                setSrcDirs(listOf("resources"))
            }
        }
        test {
            kotlin {
                setSrcDirs(listOf("test-src"))
            }
            java {
                setSrcDirs(listOf("test-src"))
            }
            resources {
                setSrcDirs(listOf("test-resources"))
            }
        }
    }

    dependencies {
        // kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect:${Version.KOTLIN}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
        testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.4")
    }

    dependencyManagement {
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }

        imports {
//            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:${Version.ALIBABA_CLOUD}")
        }
        dependencies {
            // soul
            dependency("org.soul:soul-base:${Version.SOUL}")
            dependency("org.soul:soul-context:${Version.SOUL}")
            dependency("org.soul:soul-ability-data-rdb-jdbc:${Version.SOUL}")

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
        }
    }

}