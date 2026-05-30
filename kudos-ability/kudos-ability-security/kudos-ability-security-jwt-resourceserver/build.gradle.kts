dependencies {
    api(project(":kudos-ability:kudos-ability-security:kudos-ability-security-jwt"))

    // Spring Security web filter chain + OAuth2 Resource Server JWT DSL. Pulled as api so apps
    // configuring the SecurityFilterChain in their own code don't need to redeclare these deps.
    api(libs.spring.security.config)
    api(libs.spring.security.web)
    api(libs.spring.security.oauth2.resource.server)

    testImplementation(project(":kudos-test:kudos-test-common"))
    // WebApplicationContextRunner + Spring Security web need jakarta.servlet on the test
    // classpath. Production scope doesn't include it — apps embed via spring-boot-starter-web.
    testImplementation(libs.servlet.api)
    // Spring Boot's SecurityAutoConfiguration provides the HttpSecurity bean the autoconfig
    // under test depends on. Apps don't get this transitively from our prod deps; they wire
    // it themselves via spring-boot-starter-security. Test-side dep keeps prod scope minimal.
    testImplementation(libs.spring.boot.starter.security)
}
