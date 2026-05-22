dependencies {
    api(project(":kudos-base"))
    api(libs.controlsfx)

    testImplementation(project(":kudos-test:kudos-test-common"))
}

plugins {
    alias(libs.plugins.javafx)
}

val libsCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javafxVersion: String = libsCatalog.findVersion("javafx").get().requiredVersion

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}
