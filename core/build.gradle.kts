plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.glossaCore)
    compileOnly(libs.alexandriaCore)
    compileOnly(libs.configurateCore)
    compileOnly(libs.cloudCore)
    compileOnly(libs.adventureExtraKotlin)
    compileOnly(libs.configurateExtraKotlin)

    testImplementation(kotlin("test"))
}
