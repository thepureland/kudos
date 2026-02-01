// The settings
// is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins, dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "kudos"

include("kudos-base")

include("kudos-context")

include("kudos-tools")

// test
include("kudos-test:kudos-test-common")
include("kudos-test:kudos-test-container")
include("kudos-test:kudos-test-rdb")
include("kudos-test:kudos-test-api:kudos-test-api-contract:kudos-test-api-contract-provider")
include("kudos-test:kudos-test-api:kudos-test-api-contract:kudos-test-api-contract-consumer")

// cache
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-common")
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine")
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis")
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common")
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client")
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider")

// comm
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-common")
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-email")
//include("kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aliyun")
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aws")
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket:kudos-ability-comm-websocket-ktor")

// data
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc")
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway")
include("kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis")

// distributed
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config:kudos-ability-distributed-config-nacos")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-redisson")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-kafka")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rabbit")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rocketmq")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-mq")
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-tx:kudos-ability-distributed-tx-seata")

// file
include("kudos-ability:kudos-ability-file:kudos-ability-file-common")
include("kudos-ability:kudos-ability-file:kudos-ability-file-local")
include("kudos-ability:kudos-ability-file:kudos-ability-file-minio")

// log
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common")
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mq")
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb")
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common")
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-ktorm")


// ui
include("kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx")

// web
include("kudos-ability:kudos-ability-web:kudos-ability-web-common")
include("kudos-ability:kudos-ability-web:kudos-ability-web-springmvc")
include("kudos-ability:kudos-ability-web:kudos-ability-web-ktor")


// ams
//// ams-sys
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-core")
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-common")
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-client")
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-api-public")
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-api-admin")
include("kudos-ams:kudos-ams-sys:kudos-ams-sys-api-internal")
//// ams-auth
include("kudos-ams:kudos-ams-auth:kudos-ams-auth-core")
include("kudos-ams:kudos-ams-auth:kudos-ams-auth-common")
include("kudos-ams:kudos-ams-auth:kudos-ams-auth-client")
include("kudos-ams:kudos-ams-auth:kudos-ams-auth-api-public")
include("kudos-ams:kudos-ams-auth:kudos-ams-auth-api-internal")
//// ams-user
include("kudos-ams:kudos-ams-user:kudos-ams-user-core")
include("kudos-ams:kudos-ams-user:kudos-ams-user-common")
include("kudos-ams:kudos-ams-user:kudos-ams-user-client")
include("kudos-ams:kudos-ams-user:kudos-ams-user-api-public")
include("kudos-ams:kudos-ams-user:kudos-ams-user-api-admin")
include("kudos-ams:kudos-ams-user:kudos-ams-user-api-internal")
//// ams-msg
include("kudos-ams:kudos-ams-msg:kudos-ams-msg-core")
include("kudos-ams:kudos-ams-msg:kudos-ams-msg-common")
include("kudos-ams:kudos-ams-msg:kudos-ams-msg-client")
include("kudos-ams:kudos-ams-msg:kudos-ams-msg-api-public")
include("kudos-ams:kudos-ams-msg:kudos-ams-msg-api-internal")

// dependencies
include("kudos-dependencies")
