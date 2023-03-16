dependencies {
    api(project(":kudos-base"))
    api("org.springframework.boot:spring-boot-starter-aop:3.0.4")
    api("org.springframework.boot:spring-boot-starter-test:3.0.4")

    // h2
    // h2可以用PostgreSqlDialect来实现分页
    api("com.h2database:h2:2.1.214")

    // postgres
    api("org.postgresql:postgresql:42.2.20")
    api("org.ktorm:ktorm-support-postgresql")
}