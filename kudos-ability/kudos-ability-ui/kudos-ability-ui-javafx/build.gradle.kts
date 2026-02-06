dependencies {
    api(project(":kudos-base"))
    api(libs.controlsfx)
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

