plugins {
    id("maven-publish")
}

dependencies {
    compileOnly("com.gitlab.aecsocket.minecommons", "core", "1.2-SNAPSHOT")
    compileOnly("org.spongepowered", "configurate-hocon", "4.1.1")
    compileOnly("com.google.guava", "guava", "30.1.1-jre")
    compileOnly("net.kyori", "adventure-api", "4.8.1")
    compileOnly("net.kyori", "adventure-serializer-configurate4", "4.8.1")
    compileOnly("org.checkerframework", "checker-qual", "3.15.0")
    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
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
