plugins {
    id("org.springframework.cloud.contract") version "5.0.0"
}

dependencies {
    implementation(project(":kudos-context"))
    api(project(":kudos-test:kudos-test-common"))
    api("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    api(libs.spring.boot.starter.webmvc.test)

    testImplementation(libs.spring.boot.starter.web)
}

// Spring Cloud Contract 插件配置
contracts {
    // 契约文件目录（默认就是这个，可不写）
    contractsDslDir.set(file("test-resources/contracts"))

    // 生成的测试使用哪个基类
    baseClassForTests.set("com.example.contract.BaseContractTest")

    // 是否生成 stubs
    failOnNoContracts.set(false)
}