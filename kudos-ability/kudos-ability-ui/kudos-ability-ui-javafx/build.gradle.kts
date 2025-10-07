dependencies {
    api(project(":kudos-base"))
    api(libs.controlsfx)
}

plugins {
    alias(libs.plugins.javafx)
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javafx_version = libsCatalog.findVersion("javafx").get().requiredVersion

javafx {
    version = javafx_version
    modules = listOf("javafx.controls", "javafx.fxml")
}

