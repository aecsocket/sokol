plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit")
}

/*buildscript {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        /*classpath("org.jetbrains.dokka:dokka-base:1.6.21") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
            exclude(group = "com.fasterxml.jackson.dataformat")
        }
        classpath("org.jetbrains.dokka:dokka-core:1.6.21") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
            exclude(group = "com.fasterxml.jackson.dataformat")
        }
        classpath("org.jetbrains.dokka:dokka-analysis:1.6.21") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
            exclude(group = "com.fasterxml.jackson.dataformat")
        }*/
        classpath("net.minecrell:plugin-yml:0.5.1")
    }
}

apply(plugin = "net.minecrell.plugin-yml.bukkit")*/

val minecraftVersion = libs.versions.minecraft.get()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.sokolCore)
    paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")
    implementation(libs.alexandriaPaper) { artifact { classifier = "reobf" } }
    implementation(libs.bstatsPaper)
    implementation(libs.packetEvents)
    implementation(libs.adventureExtraKotlin)

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }
}

bukkit {
    name = "Sokol"
    main = "${project.group}.paper.SokolPlugin"
    apiVersion = "1.18"
    authors = listOf("aecsocket")
    website = "https://aecsocket.github.com/sokol"
}
