plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "com.gitlab.aecsocket.sokol"
    version = "1.2-SNAPSHOT"
    description = "Platform-agnostic, data-driven item framework"
}

subprojects {
    apply<JavaLibraryPlugin>()

    java {
        targetCompatibility = JavaVersion.toVersion(16)
        sourceCompatibility = JavaVersion.toVersion(16)
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://gitlab.com/api/v4/projects/27049637/packages/maven")
    }

    dependencies {
        testImplementation("org.junit.jupiter", "junit-jupiter", "5.7.1")
    }

    tasks {
        jar {
            archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
        }

        compileJava {
            options.encoding = Charsets.UTF_8.name()
            options.release.set(16)
        }

        javadoc {
            options.encoding = Charsets.UTF_8.name()
            source = sourceSets.main.get().allJava
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
            name = "gitlab"
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
