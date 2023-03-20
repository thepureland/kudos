dependencies {
    implementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
    implementation(project(":kudos-ability::kudos-ability-ui::kudos-ability-ui-javafx"))
    implementation("org.freemarker:freemarker")

    testImplementation(project(":kudos-test:kudos-test-common"))
}

javafx {
    version = "11"
    modules("javafx.controls", "javafx.fxml")
//    configuration = "compileOnly"
}