dependencies {
    implementation(project(":kudos-base"))
    api("org.controlsfx:controlsfx")
//    api("de.roskenet:springboot-javafx-support")
}

javafx {
    version = "11"
    modules("javafx.controls", "javafx.fxml")
//    configuration = "compileOnly"
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("resources"))
        }
    }
    test {
        kotlin {
            setSrcDirs(listOf("test-src"))
        }
        java {
            setSrcDirs(listOf("test-src"))
        }
        resources {
            setSrcDirs(listOf("test-resources"))
        }
    }
}