dependencies {
    api(project(":kudos-base"))
    api("org.controlsfx:controlsfx:8.40.10")
}

plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
}

val javafxVersion = "21"

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

