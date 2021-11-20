plugins {
    id("maven-publish")
}

dependencies {
    compileOnly("com.gitlab.aecsocket.minecommons", "core", "1.3") { isTransitive = true }
    implementation("org.incendo.interfaces", "interfaces-core", "1.0.0-SNAPSHOT")
}

tasks {
    javadoc {
        val opt = options as StandardJavadocDocletOptions
        opt.links(
                "https://docs.oracle.com/en/java/javase/16/docs/api/",
                "https://aecsocket.gitlab.io/minecommons/javadoc/core/"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("gitlab") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://gitlab.com/api/v4/projects/27149151/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
