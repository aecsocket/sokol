plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
}

val minecraft = libs.versions.minecraft.get()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.sokolCore)
    paperDevBundle("$minecraft-R0.1-SNAPSHOT")

    implementation(libs.glossaCore) { isTransitive = false }
    implementation(libs.glossaAdventure) { isTransitive = false }
    implementation(libs.glossaConfigurate) { isTransitive = false }

    implementation(libs.alexandriaCore)
    implementation(libs.alexandriaPaper)

    // shaded

    implementation(libs.bstatsPaper)
    implementation(libs.packetEventsApi)
    implementation(libs.packetEventsSpigot)

    // library loader

    compileOnly(libs.kotlinStdlib)
    compileOnly(libs.kotlinReflect)
    compileOnly(libs.configurateCore)
    compileOnly(libs.cloudPaper)
    compileOnly(libs.configurateExtraKotlin)
    compileOnly(libs.adventureExtraKotlin)

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        mergeServiceFiles()
        exclude("kotlin/")
        listOf(
            "com.github.retrooper.packetevents",
            "io.github.retrooper.packetevents",
            "org.bstats",
            "com.google.gson",
            "org.jetbrains",
            "org.intellij"
        ).forEach { relocate(it, "${project.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(minecraft)
    }
}
