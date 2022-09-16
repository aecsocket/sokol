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
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.sokolCore)
    paperDevBundle("$minecraft-R0.1-SNAPSHOT")

    compileOnly(libs.adventureExtraKotlin)

    compileOnly(libs.configurateCore)
    compileOnly(libs.configurateHocon)
    compileOnly(libs.configurateExtraKotlin)

    compileOnly(libs.cloudPaper)
    compileOnly(libs.cloudMinecraftExtras) { isTransitive = false }

    compileOnly(libs.glossaCore)

    compileOnly(libs.alexandriaCore)
    compileOnly(libs.alexandriaPaper)

    compileOnly(libs.packetEventsSpigot)

    compileOnly(libs.craftbulletPaper)

    // shaded
    implementation(libs.bstatsPaper)


    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    shadowJar {
        mergeServiceFiles()
        exclude("kotlin/")
        exclude("kotlinx/")
        listOf(
            "org.bstats",

            "org.jetbrains",
            "org.intellij",
        ).forEach { relocate(it, "${project.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(minecraft)
    }
}
