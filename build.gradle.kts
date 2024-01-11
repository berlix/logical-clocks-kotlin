import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    kotlin("multiplatform") version "1.9.21"
    id("maven-publish")
    signing
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("com.palantir.git-version") version "3.0.0"
    id("io.kotest.multiplatform") version "5.6.2"
    id("org.jetbrains.dokka") version "1.9.10"
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra

fun version(): String = versionDetails().run {
    if (commitDistance == 0 && isCleanTag && lastTag.matches(Regex("""\d+\.\d+\.\d+""")))
        version
    else (
            System.getenv("GITHUB_RUN_NUMBER")?.let { "ci-${branchName}-$it-${gitHash}" }
                ?: "dev-${branchName}-${
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC")).format(Instant.now())
                }-${gitHash}"
            )
}

group = "pro.felixo"
version = version()

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(11)

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    @Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:5.6.2")
                implementation("io.kotest:kotest-framework-datatest:5.6.2")
                implementation("com.willowtreeapps.assertk:assertk:0.26.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5-jvm:5.6.2")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

detekt {
    source.setFrom(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmMain/kotlin",
        "src/jvmTest/kotlin",
        "src/jsMain/kotlin",
        "src/jsTest/kotlin"
    )
}

publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_TOKEN")
            }
        }
    }

    publications.withType<MavenPublication> {
        val javadocJar = tasks.register("javadocJar$name", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveBaseName.set("javadoc-${this@withType.name}")
            from(tasks.dokkaHtml)
        }

        artifact(javadocJar.get())

        pom {
            name = rootProject.name
            description = "Logical clocks for Kotlin Multiplatform"
            url = "https://github.com/berlix/logical-clocks-kotlin"
            licenses {
                license {
                    name = "MIT License"
                    url = "https://opensource.org/license/mit/"
                }
            }
            developers {
                developer {
                    id = "berlix"
                    name = "Felix Engelhardt"
                    email = "pub@felix-engelhardt.de"
                }
            }
            scm {
                connection = "scm:git:git://github.com/berlix/logical-clocks-kotlin.git"
                developerConnection = "scm:git:git@github.com:berlix/logical-clocks-kotlin.git"
                url = "https://github.com/berlix/logical-clocks-kotlin"
            }
        }

        signing {
            useInMemoryPgpKeys(System.getenv("OSSRH_GPG_KEY"), System.getenv("OSSRH_GPG_PASSWORD"))
            sign(this@withType)
        }
    }
}
