dependencies {
    api(project(":kudos-base"))
    api("org.controlsfx:controlsfx:8.40.10")
}

plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
}

val javafxVersion = "21"

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

