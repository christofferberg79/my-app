import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.3.11"
    application
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

val ktorVersion = "1.1.1"
val logbackVersion = "1.2.3"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0"
}

tasks.create("stage") {
    dependsOn("build")
}

tasks.withType<ShadowJar> {
    classifier = ""
    version = ""
}