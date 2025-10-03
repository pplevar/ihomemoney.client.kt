plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.20"
}

group = "ru.levar"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0") // Для JSON (Gson)

    // OkHttp (для работы сетевых запросов)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // Логирование

    // Kotlin Coroutines (для асинхронности)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Kotest testing framework
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
}

tasks.test {
    useJUnitPlatform()
}

// ============================================================================
// Maven Publishing Configuration
// ============================================================================

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaHtml"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "ihomemoney-client-kt"
            version = project.version.toString()

            pom {
                name.set("iHomemoney Kotlin Client")
                description.set("A Kotlin-based REST API client for the iHomemoney personal finance service")
                url.set("https://github.com/pplevar/ihomemoney.client.kt")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("pplevar")
                        name.set("Leonid Karavaev")
                        email.set("pplevar@users.noreply.github.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/pplevar/ihomemoney.client.kt.git")
                    developerConnection.set("scm:git:ssh://github.com/pplevar/ihomemoney.client.kt.git")
                    url.set("https://github.com/pplevar/ihomemoney.client.kt")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/pplevar/ihomemoney.client.kt")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// ============================================================================
// Publishing Tasks
// ============================================================================

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publishes a SNAPSHOT version to GitHub Packages"

    doFirst {
        if (!project.version.toString().endsWith("-SNAPSHOT")) {
            throw GradleException("Version must end with -SNAPSHOT for snapshot publishing")
        }
    }

    dependsOn("publish")
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes a release version to GitHub Packages"

    doFirst {
        if (project.version.toString().endsWith("-SNAPSHOT")) {
            throw GradleException("Version must not end with -SNAPSHOT for release publishing")
        }
        if (project.version.toString() == "unspecified") {
            throw GradleException("Version must be specified for release publishing")
        }
    }

    dependsOn("publish")
}
