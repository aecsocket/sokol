plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("xyz.jpenilla.run-paper") version "1.0.3"
}

val minecraftVersion = "1.17.1"

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
    mavenLocal()
}

dependencies {
    api(project(":core"))
    compileOnly("io.papermc.paper", "paper-api", "1.17.1-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
    }

    implementation("org.bstats", "bstats-bukkit", "2.2.1")

    compileOnly("org.spongepowered", "configurate-hocon", "4.1.1")
    val cloudVersion = "1.4.0"
    compileOnly("cloud.commandframework", "cloud-paper", cloudVersion)
    compileOnly("cloud.commandframework", "cloud-minecraft-extras", cloudVersion)
    compileOnly("com.github.stefvanschie.inventoryframework", "IF", "0.10.0")

    // Plugins
    compileOnly("com.gitlab.aecsocket.minecommons", "paper", "1.2-SNAPSHOT")
    compileOnly("com.comphenix.protocol", "ProtocolLib", "4.7.0")
}

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
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
