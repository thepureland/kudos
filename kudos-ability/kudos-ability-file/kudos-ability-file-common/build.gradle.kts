dependencies {
    api(project(":kudos-context"))
    api(libs.sejda.webp.imageio)
    api(libs.coobird.thumbnailator)
    api(libs.xqlee.pngquant.png)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
