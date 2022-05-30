plugins {
    kotlin("jvm")
    id("maven-publish")
    //id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.sokol"
    version = "2.2.0"
    description = "Platform-agnostic, data-driven item framework"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
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
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
