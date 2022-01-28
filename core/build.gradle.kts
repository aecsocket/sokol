plugins {
    id("java")
    id("maven-publish")
}

dependencies {
    compileOnly(libs.minecommons)
    implementation(libs.interfaces)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.minecommons)
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aecsocket/sokol")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
