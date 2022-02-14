plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    api(projects.sokolCore) {
        exclude("com.github.aecsocket", "minecommons-core")
    }
    compileOnly(libs.paper) {
        exclude("junit", "junit")
    }

    implementation(libs.minecommonsPaper)
    implementation(libs.bstatsPaper)

    compileOnly(libs.protocolLib)
}

tasks {
    shadowJar {
        listOf(
            "io.leangen.geantyref",
            "org.spongepowered.configurate",
            "com.typesafe.config",
            "au.com.bytecode.opencsv",
            "cloud.commandframework",
            "net.kyori.adventure.text.minimessage",
            "net.kyori.adventure.serializer.configurate4",
            "com.github.stefvanschie.inventoryframework",
            "com.github.aecsocket.minecommons",
            "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.${rootProject.name}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.forUseAtConfigurationTime().get())
    }
}

bukkit {
    name = "Sokol"
    main = "${project.group}.${rootProject.name}.paper.SokolPlugin"
    apiVersion = "1.18"
    depend = listOf("ProtocolLib")
    website = "https://github.com/aecsocket/sokol"
    authors = listOf("aecsocket")
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aecsocket/sokol")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
