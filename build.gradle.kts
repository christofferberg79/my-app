import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("kotlin-multiplatform") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("org.liquibase.gradle") version "2.0.1"
    id("net.saliman.properties") version "1.5.1"
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven("http://kotlin.bintray.com/ktor")
}

val ktorVersion = "1.1.5"
val logbackVersion = "1.2.3"
val exposedVersion = "0.13.6"
val postgresqlDriverVersion = "42.2.5"
val liquibaseVersion = "3.6.3"
val liquibaseGroovyDslVersion = "2.0.3"

val jdbcDatabaseUrl: String? by project

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:$liquibaseVersion")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
    liquibaseRuntime("org.postgresql:postgresql:$postgresqlDriverVersion")
}

kotlin {
    jvm {
        compilations.getByName("main") {
            tasks.register<JavaExec>("run") {
                group = "run"
                dependsOn("jvmMainClasses")
                environment("JDBC_DATABASE_URL", jdbcDatabaseUrl ?: "")
                classpath(
                    output.allOutputs.files,
                    runtimeDependencyFiles
                )
                main = "io.ktor.server.netty.EngineMain"
            }

            tasks.register<ShadowJar>("shadowJar") {
                group = "build"

                from(output)
                configurations = listOf(project.configurations["jvmRuntimeClasspath"])

                archiveClassifier.set("")
                archiveVersion.set("")

                manifest {
                    attributes("Main-Class" to "io.ktor.server.netty.EngineMain")
                }
            }
        }
    }

    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-jackson:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("org.jetbrains.exposed:exposed:$exposedVersion")
                implementation("org.postgresql:postgresql:$postgresqlDriverVersion")
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-server-test-host:$ktorVersion")
                implementation("com.github.stefanbirkner:system-rules:1.19.0")
                implementation("org.liquibase:liquibase-core:$liquibaseVersion")
                implementation("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
                implementation("org.hamcrest:hamcrest-library:2.1")
            }
        }
    }
}

tasks.wrapper {
    gradleVersion = "5.4.1"
}

tasks.register<Copy>("copyLiquibase") {
    group = "heroku setup"
    from(configurations.liquibaseRuntime)
    into("$buildDir/libs/liquibase")
    rename("-\\d+(\\.\\d+)*\\.jar$", ".jar")
}

tasks.register("stage") {
    group = "heroku setup"
    dependsOn("shadowJar", "copyLiquibase")
}

tasks.dependencyUpdates {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "eap")
                    .map { Regex(".*[.-]$it[.\\d-]*", RegexOption.IGNORE_CASE) }
                    .any { candidate.version.matches(it) }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }
}

liquibase {
    activities {
        register("main") {
            arguments = mapOf(
                "url" to jdbcDatabaseUrl,
                "changeLogFile" to "db/changelog.groovy"
            )
        }
    }
}
