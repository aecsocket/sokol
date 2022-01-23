plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper")
}

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
}

dependencies {
    api(project(":${rootProject.name}-core"))
    compileOnly(libs.paperApi)
    //compileOnly("io.papermc.paper", "paper-api", "1.17.1-R0.1-SNAPSHOT") {
    //    exclude("junit", "junit")
    //}

    implementation(libs.bstats)
    implementation(libs.interfacesPaper)

    compileOnly(libs.minecommonsPaper)
    compileOnly(libs.protocolLib)

    //implementation("org.bstats", "bstats-bukkit", "2.2.1")
    //implementation("org.incendo.interfaces", "interfaces-paper", "1.0.0-SNAPSHOT")

    // Plugins
    //compileOnly("com.gitlab.aecsocket.minecommons", "paper", "1.3")
    //compileOnly("com.comphenix.protocol", "ProtocolLib", "4.7.0")
}

tasks {
    shadowJar {
        listOf(
                "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.18.1")
    }
}

bukkit {
    name = "Sokol"
    main = "${project.group}.${rootProject.name}.paper.SokolPlugin"
    apiVersion = "1.18"
    depend = listOf("Minecommons", "ProtocolLib")
    website = "https://github.com/aecsocket/sokol"
    authors = listOf("aecsocket")
}
