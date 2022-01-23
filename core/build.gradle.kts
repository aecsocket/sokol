plugins {
    id("maven-publish")
}

dependencies {
    compileOnly(libs.minecommonsCore)
    implementation(libs.interfacesCore)

    testImplementation(libs.bundles.junit)
}

java {
    withJavadocJar()
    withSourcesJar()
}
