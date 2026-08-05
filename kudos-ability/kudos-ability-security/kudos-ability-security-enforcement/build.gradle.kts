dependencies {
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))

    // Servlet filter + @ConfigurationProperties. api: the filter and its properties are the module's
    // public surface, and apps register the filter in their own SecurityFilterChain.
    api(libs.spring.boot.starter.web)

    // Method-level enforcement (@RequiresPermission) is an aspect.
    api(libs.spring.boot.starter.aop)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
