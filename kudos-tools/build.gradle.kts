dependencies {
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    implementation(project(":kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx"))

    implementation(libs.freemarker)
    implementation(libs.h2database.h2)
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