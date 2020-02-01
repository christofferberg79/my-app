import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.frontend.webpack.WebPackExtension

plugins {
    kotlin("multiplatform") version "1.3.50"
    kotlin("frontend") version "0.0.45"
    id("kotlinx-serialization") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("org.liquibase.gradle") version "2.0.2"
    id("net.saliman.properties") version "1.5.1"
}

group = "cberg"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlin-js-wrappers")
}

val ktorVersion = "1.2.4"
val logbackVersion = "1.2.3"
val exposedVersion = "0.17.7"
val postgresqlDriverVersion = "42.2.9"
val liquibaseVersion = "3.8.5"
val liquibaseGroovyDslVersion = "2.1.1"
val kotlinReactVersion = "16.9.0-pre.83-kotlin-1.3.50"
val kotlinStyledVersion = "1.0.0-pre.83-kotlin-1.3.50"
val hamcrestLibraryVersion = "2.2"

val jdbcDatabaseUrl: String? by project

kotlin {
    targets {
        jvm {
            compilations.getByName("main") {
                tasks {
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
            compilations.getByName("main") {
                compileKotlinTask.kotlinOptions {
                    sourceMap = true
                    moduleKind = "commonjs"
                }
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
        gradleVersion = "5.6.2"
    }

    register<Copy>("copyLiquibase") {
        group = "heroku setup"
        from(configurations.liquibaseRuntime)
        into("$buildDir/libs/liquibase")
        rename("-\\d+(\\.\\d+)*\\.jar$", ".jar")
    }

    register<Copy>("copyBundleToKtor") {
        group = "heroku setup"
        dependsOn("bundle")
        from("$buildDir/bundle")
        from("src/jsMain/web")
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

kotlinFrontend {
    downloadNodeJsVersion = "10.15.0"

    sourceMaps = true

    bundle("webpack", delegateClosureOf<WebPackExtension> {
        bundleName = "main"
        proxyUrl = "http://localhost:8080"
        contentPath = file("src/jsMain/web")
    })

    npm {
        devDependency("text-encoding") // workaround for https://github.com/ktorio/ktor/issues/961
        devDependency("core-js")
        dependency("react")
        dependency("react-dom")
        dependency("@jetbrains/kotlin-css")
        dependency("@jetbrains/kotlin-css-js")
        dependency("@jetbrains/kotlin-styled")
        dependency("inline-style-prefixer")
        dependency("styled-components")
    }
}

ktor {
    port = 8080
    mainClass = "io.ktor.server.netty.EngineMain"
    jvmOptions = arrayOf("-DjdbcDatabaseUrl=$jdbcDatabaseUrl")
}
