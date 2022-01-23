plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    api(projects.sokolCore)
    compileOnly(libs.paper)

    implementation(libs.paperBstats)
    implementation(libs.paperInterfaces)

    compileOnly(libs.paperMinecommons)
    compileOnly(libs.paperProtocolLib)
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
