dependencies {
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    implementation(project(":kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx"))

    api("org.freemarker:freemarker:2.3.30")
}


plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
}

val javafxVersion = "21"

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}