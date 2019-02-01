import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20"
    application
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.liquibase.gradle") version "2.0.1"
    id("net.saliman.properties") version "1.4.6"
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
}

val ktorVersion = "1.1.2"
val logbackVersion = "1.2.3"
val exposedVersion = "0.12.1"
val postgresqlDriverVersion = "42.2.5"
val liquibaseVersion = "3.6.3"
val liquibaseGroovyDslVersion = "2.0.2"
val h2Version = "1.4.197"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.exposed:exposed:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresqlDriverVersion")

    liquibaseRuntime("org.liquibase:liquibase-core:$liquibaseVersion")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
    liquibaseRuntime("org.postgresql:postgresql:$postgresqlDriverVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.liquibase:liquibase-core:$liquibaseVersion")
    testImplementation("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
    testImplementation("org.hamcrest:hamcrest-library:2.1")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.wrapper {
    gradleVersion = "5.1.1"
}

tasks.register("stage") {
    dependsOn("clean", "build", "copyLiquibase")
}

tasks.named("build") {
    mustRunAfter("clean")
}

tasks.register<Copy>("copyLiquibase") {
    from(configurations.liquibaseRuntime)
    into("$buildDir/libs/liquibase")
    rename("-\\d+(\\.\\d+)*\\.jar$", ".jar")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.dependencyUpdates {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview")
                    .map { Regex(".*[.-]$it[.\\d-]*", RegexOption.IGNORE_CASE) }
                    .any { candidate.version.matches(it) }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }
}

val jdbcDatabaseUrl: String by project

liquibase {
    activities {
        register("main") {
            arguments = mapOf(
                "url" to jdbcDatabaseUrl,
                "changeLogFile" to "src/main/resources/db/changelog.groovy"
            )
        }
    }
}
