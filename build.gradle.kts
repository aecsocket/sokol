plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "com.gitlab.aecsocket.sokol"
    version = "1.2"
    description = "Platform-agnostic, data-driven item framework"
}

subprojects {
    apply<JavaLibraryPlugin>()

    java {
        targetCompatibility = JavaVersion.toVersion(16)
        sourceCompatibility = JavaVersion.toVersion(16)
    }

    repositories {
        //mavenLocal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://gitlab.com/api/v4/projects/27049637/packages/maven") // Minecommons
    }

    dependencies {
        testImplementation("org.junit.jupiter", "junit-jupiter", "5.7.1")
    }

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name()
            options.release.set(16)
        }

        javadoc {
            val opt = options as StandardJavadocDocletOptions
            opt.encoding = Charsets.UTF_8.name()
            opt.source("16")
            opt.linkSource(true)
            opt.author(true)
        }

        test {
            useJUnitPlatform()
        }
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