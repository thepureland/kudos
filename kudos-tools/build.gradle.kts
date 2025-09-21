dependencies {
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway"))
    implementation(project(":kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx"))

    implementation("org.freemarker:freemarker:2.3.30")
    implementation("com.h2database:h2:${libs.versions.h2.get()}")
}


plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
}

val javafxVersion = "21"

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}