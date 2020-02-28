import java.util.Properties
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") version "1.3.61"
    kotlin("plugin.serialization") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("org.liquibase.gradle") version "2.0.2"
    java
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlin-js-wrappers")
}

val ktorVersion = "1.3.1"
val logbackVersion = "1.2.3"
val exposedVersion = "0.17.7"
val postgresqlDriverVersion = "42.2.10"
val liquibaseVersion = "3.8.7"
val liquibaseGroovyDslVersion = "2.1.1"
val kotlinReactVersion = "16.9.0-pre.91-kotlin-1.3.61"
val kotlinStyledVersion = "1.0.0-pre.91-kotlin-1.3.61"
val hamcrestLibraryVersion = "2.2"


val localProperties = Properties()
val file = rootProject.file("local.properties")
if (file.exists()) {
    localProperties.load(file.inputStream())
}
val jdbcDatabaseUrl = localProperties["jdbcDatabaseUrl"]

kotlin {
    jvm {
        compilations.getByName("main") {
            tasks {
                register<JavaExec>("jvmServerRun") {
                    classpath = output.allOutputs + configurations["jvmRuntimeClasspath"]
                    main = "io.ktor.server.netty.EngineMain"
                    jvmArgs("-DjdbcDatabaseUrl=$jdbcDatabaseUrl")
                }

                named<ShadowJar>("shadowJar") {
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
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    moduleKind = "commonjs"
                }
            }

            webpackTask {
                outputFileName = "main.bundle.js"
            }

            runTask {
                outputFileName = "main.bundle.js"
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

                implementation(npm("react", "16.9.0"))
                implementation(npm("react-dom", "16.9.0"))
                implementation(npm("react-is", "16.9.0"))
                implementation(npm("@jetbrains/kotlin-extensions", "1.0.1-pre.91"))
                implementation(npm("@jetbrains/kotlin-css", "1.0.0-pre.91"))
                implementation(npm("@jetbrains/kotlin-css-js", "1.0.0-pre.91"))
                implementation(npm("@jetbrains/kotlin-styled", "1.0.0-pre.91"))
                implementation(npm("css-in-js-utils", "3.0.2"))
                implementation(npm("core-js"))
                implementation(npm("inline-style-prefixer"))
                implementation(npm("styled-components", "4.3.2"))
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
        gradleVersion = "6.2.1"
    }

    register<Copy>("copyLiquibase") {
        group = "heroku setup"
        from(configurations.liquibaseRuntime)
        into("$buildDir/libs/liquibase")
        rename("-\\d+(\\.\\d+)*\\.jar$", ".jar")
    }

    register<Copy>("copyBundleToKtor") {
        group = "heroku setup"
        dependsOn("jsBrowserWebpack", "jsProcessResources")
        from("$buildDir/distributions")
        from("$buildDir/processedResources/js/main")
        into("$buildDir/processedResources/jvm/main/web")
    }

    register("stage") {
        group = "heroku setup"
        dependsOn("copyBundleToKtor", "shadowJar", "copyLiquibase")
    }

    named("shadowJar") {
        mustRunAfter("copyBundleToKtor")
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
