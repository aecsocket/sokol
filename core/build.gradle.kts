plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
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
