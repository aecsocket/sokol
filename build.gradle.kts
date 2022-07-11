plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.sokol"
    version = "2.2.1"
    description = "Platform-agnostic, data-driven item framework"
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    tasks {
        test {
            useJUnitPlatform()
        }

        processResources {
            filter { it
                .replace("%version%", project.version.toString())
                .replace("%description%", project.description.toString())
                .replace("%group%", project.group.toString())
            }
        }
    }
}
