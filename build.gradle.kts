object Version {
    const val KOTLIN = "1.8.10"
    const val SOUL = "5.0.0.0-SNAPSHOT"
    const val KTORM = "3.6.0"
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

    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
        google()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://mvnrepository.com/") }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://nexus.soulworld.net/repository/maven-public")
        }
        mavenLocal()
    }

    apply {
        plugin("kotlin")
        plugin("idea")
//        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }

    // 为了不让主工程生成src相关的目录
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
//        all*.exclude group: 'commons-logging'
//        all*.exclude group: 'commons-beanutils', module: 'commons-beanutils'
    }

    // 显示当前项目下所有用于 compile 的 jar.
//    tasks.listJars(description: 'Display all compile jars.') << {
//       configurations.compile.each { File file -> println file.name }
//    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

//    tasks.withType<BootJar> {
//        enabled = false
//    }

    tasks.withType<Jar> {
        enabled = true
    }

//    tasks.withType<KotlinCompile> {
//        kotlinOptions {
//            freeCompilerArgs = listOf("-Xjsr305=strict")
//            jvmTarget = "17"
//        }
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
                setSrcDirs(listOf("test"))
            }
            java {
                setSrcDirs(listOf("test"))
            }
            resources {
                setSrcDirs(listOf("testresources"))
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
//            mavenBom("org.springframework.boot:spring-boot-dependencies:${Version.SPRING_BOOT}")
//            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${Version.SPRING_CLOUD}")
//            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:${Version.ALIBABA_CLOUD}")
        }
        dependencies {
            // soul
            dependency("org.soul:soul-base:${Version.SOUL}")

            // commons
//            dependency("org.apache.commons:commons-lang3:3.10")
//            dependency("org.apache.commons:commons-text:1.8")
//            dependency("commons-io:commons-io:2.6")
//            dependency("commons-fileupload:commons-fileupload:1.4")
//            dependency("commons-codec:commons-codec:1.14")
//            dependency("commons-beanutils:commons-beanutils:1.9.4")
//            dependency("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.7")
//            dependency("net.sourceforge.jexcelapi:jxl:2.6.12")
//            dependency("javax.xml.bind:jaxb-api:2.3.1")
//            dependency("com.sun.xml.bind:jaxb-impl:2.3.1")
//            dependency("org.glassfish.jaxb:jaxb-runtime:2.3.1")
//            dependency("com.google.zxing:core:3.4.0")
//            dependency("org.javamoney:moneta:1.4.2")
//            dependency("org.apache.xmlgraphics:batik-all:1.1.4")
//            dependency("xerces:xercesImpl:2.12.1")

            // validation
//            dependency("javax.validation:validation-api:2.0.1.Final")
            dependency("org.hibernate.validator:hibernate-validator:6.1.5.Final")
            dependency("javax.el:javax.el-api:3.0.1-b06")
            dependency("org.glassfish.web:javax.el:2.2.6")

            // data
            dependency("org.ktorm:ktorm-core:${Version.KTORM}")
            dependency("org.ktorm:ktorm-jackson:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-mysql:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-postgresql:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-oracle:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-sqlserver:${Version.KTORM}")
            dependency("org.ktorm:ktorm-support-sqlite:${Version.KTORM}")

            // session
            dependency("org.springframework.session:spring-session-data-redis:2.2.2.RELEASE")

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

            // 依赖maven中不存在的jar
//            ext.jarTree = fileTree(dir: 'libs', include: '**/*.jar')
//            ext.rootProjectLibs = new File(rootProject.rootDir, 'libs').getAbsolutePath()
//            ext.jarTree += fileTree(dir: rootProjectLibs, include: '**/*.jar')
//            compile jarTree
        }
    }

}