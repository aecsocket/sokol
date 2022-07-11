plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
}

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
    implementation(libs.packetEventsApi)
    implementation(libs.packetEventsSpigot)
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
