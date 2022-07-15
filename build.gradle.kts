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
    apply(plugin = "org.jetbrains.dokka")

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }

        test {
            useJUnitPlatform()
        }

        processResources {
            filteringCharset = Charsets.UTF_8.name()
            filter { it
                .replace("@version@", project.version.toString())
                .replace("@description@", project.description.toString())
                .replace("@group@", project.group.toString())
                .replace("@kotlin-version@", libs.versions.kotlin.get())
                .replace("@icu4j-version@", libs.versions.icu4j.get())
                .replace("@adventure-version@", libs.versions.adventure.get())
                .replace("@configurate-version@", libs.versions.configurate.get())
                .replace("@cloud-version@", libs.versions.cloud.get())
            }
        }
    }
}
