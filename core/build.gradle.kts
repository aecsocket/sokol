plugins {
    id("maven-publish")
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
}

dependencies {
    compileOnly(libs.minecommonsCore)
    implementation(libs.interfacesCore)
    //compileOnly("com.gitlab.aecsocket.minecommons", "core", "1.3") { isTransitive = true }
    //implementation("org.incendo.interfaces", "interfaces-core", "1.0.0-SNAPSHOT")
}

java {
    withJavadocJar()
    withSourcesJar()
}
