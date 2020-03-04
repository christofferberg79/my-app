import java.util.Properties
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") version "1.3.70"
    kotlin("plugin.serialization") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("org.liquibase.gradle") version "2.0.2"
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlin-js-wrappers")
}

loadLocalProperties()

val jdbcDatabaseUrl: String? by project
val distsDir: File by project
val libsDir: File by project

val ktorVersion = "1.3.1"
val logbackVersion = "1.2.3"
val exposedVersion = "0.17.7"
val postgresqlDriverVersion = "42.2.10"
val liquibaseVersion = "3.8.7"
val liquibaseGroovyDslVersion = "2.1.1"
val reactVersion = "16.13.0"
val kotlinReactVersion = "$reactVersion-pre.92-kotlin-1.3.61"
val kotlinStyledVersion = "1.0.0-pre.92-kotlin-1.3.61"
val hamcrestLibraryVersion = "2.2"

kotlin {
    jvm {
        compilations.getByName("main") {
            tasks {
                register<JavaExec>("jvmServerRun") {
                    classpath = output.allOutputs + configurations["jvmRuntimeClasspath"]
                    main = "io.ktor.server.netty.EngineMain"
                    jvmArgs("-DjdbcDatabaseUrl=$jdbcDatabaseUrl")
                }

                register<ShadowJar>("fatJar") {
                    group = "heroku setup"

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
    }

    js {
        browser {
            // workaround for https://github.com/ktorio/ktor/issues/1339
            @Suppress("EXPERIMENTAL_API_USAGE")
            dceTask {
                keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
            }

            runTask {
                devServer = devServer?.copy(
                    port = 8088,
                    proxy = mapOf("/todos" to "http://localhost:8080")
                )
            }
        }
    }

    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-html-builder:$ktorVersion")
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
                implementation("org.liquibase:liquibase-core:$liquibaseVersion")
                implementation("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
                implementation("org.hamcrest:hamcrest-library:$hamcrestLibraryVersion")
            }
        }

        getByName("jsMain") {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains:kotlin-react:$kotlinReactVersion")
                implementation("org.jetbrains:kotlin-react-dom:$kotlinReactVersion")
                implementation("org.jetbrains:kotlin-styled:$kotlinStyledVersion")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")

                implementation(npm("text-encoding", "0.7.0"))
                implementation(npm("abort-controller", "3.0.0"))

                implementation(npm("react", reactVersion))
                implementation(npm("react-dom", reactVersion))
                implementation(npm("react-is", reactVersion))
                implementation(npm("inline-style-prefixer", "5.1.2"))
                implementation(npm("styled-components", "5.0.1"))
            }
        }
    }
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:$liquibaseVersion")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:$liquibaseGroovyDslVersion")
    liquibaseRuntime("org.postgresql:postgresql:$postgresqlDriverVersion")
}

liquibase {
    activities.register("main") {
        arguments = mapOf(
            "url" to jdbcDatabaseUrl,
            "changeLogFile" to "db/changelog.groovy"
        )
    }
}

tasks {
    wrapper {
        gradleVersion = "6.2.2"
    }

    register<Copy>("copyLiquibase") {
        group = "heroku setup"
        from(configurations.liquibaseRuntime)
        into("$libsDir/liquibase")
        rename("-\\d+(\\.\\d+)*\\.jar$", ".jar")
    }

    register<Copy>("copyJsBundleToKtor") {
        group = "heroku setup"
        from("$distsDir")
        into("$buildDir/processedResources/jvm/main/web")
    }

    register("stage") {
        group = "heroku setup"
        dependsOn("jsBrowserDistribution", "copyJsBundleToKtor", "fatJar", "copyLiquibase")
    }

    named("copyJsBundleToKtor") {
        mustRunAfter("jsBrowserDistribution")
    }

    named("fatJar") {
        mustRunAfter("copyJsBundleToKtor")
    }

    dependencyUpdates {
        resolutionStrategy {
            componentSelection.all {
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

fun loadLocalProperties() {
    file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
        val props = Properties().apply { load(stream) }
        for (name in props.stringPropertyNames()) {
            extra[name] = props.getProperty(name)
        }
    }
}