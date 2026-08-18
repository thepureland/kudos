dependencies {
    api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-sql"))
    // msg 自己的种子脚本 merge 进 sys_cache / sys_dict / sys_micro_service，需要 sys 的建表脚本在测试 classpath 上。
    // 只要 sql 模块，不要 sys-core 的 bean。
    testImplementation(project(":kudos-ms:kudos-ms-sys:kudos-ms-sys-sql"))
    api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-common"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-email"))
    api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aws"))

    api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-client"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis"))

    // H2
    // H2 can use PostgreSqlDialect for paging
    testImplementation(libs.h2database.h2)

    // postgres
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorm.support.postgresql)


    testImplementation(project(":kudos-test:kudos-test-rdb"))
}
