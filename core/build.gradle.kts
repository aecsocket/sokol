plugins {
    id("java")
    id("maven-publish")
}

dependencies {
    implementation(libs.minecommons)
    compileOnly(libs.findBugs)

    testImplementation(libs.bundles.junit)
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
