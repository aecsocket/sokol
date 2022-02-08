plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    api(projects.sokolCore)
    compileOnly(libs.paper) {
        exclude("junit", "junit")
    }

    implementation(libs.bstatsPaper)
    implementation(libs.interfacesPaper)

    // Plugins + library loader
    compileOnly(libs.minecommonsPaper)
    compileOnly(libs.bundles.cloudPaper)
    compileOnly(libs.protocolLib)
    compileOnly(libs.configurate)
    library(libs.bundles.libsPaper)
}

tasks {
    shadowJar {
        listOf(
            "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.${rootProject.name}.lib.$it") }
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
