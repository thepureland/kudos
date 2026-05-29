dependencies {
    api(project(":kudos-context"))

    // springdoc-openapi-starter-webmvc-api ships the OpenAPI metadata bean + /v3/api-docs endpoint
    // without the Swagger UI bundle. Apps that want the rendered UI page can additionally pull in
    // `springdoc-openapi-starter-webmvc-ui` themselves; we deliberately don't force the UI assets on
    // production builds.
    api(libs.springdoc.openapi.starter.webmvc.api)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jetty)
}
