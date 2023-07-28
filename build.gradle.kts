import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("maven-publish")
    `java-library`
    `maven-publish`
    signing
    application
}

group = "io.korpi"
version = "0.1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

buildscript {
    repositories {
        mavenLocal()
    }
}


dependencies {
    implementation("org.jdbi:jdbi3-core:3.1.0")  // SQL

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")  // Types and serialization
    //implementation("app.softwork:kotlinx-uuid-core:LATEST")

    implementation("io.arrow-kt:arrow-core:1.2.0")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0")

    implementation("com.zaxxer:HikariCP:5.0.1")  // Testing
    testImplementation(kotlin("test"))

    // Not required for the library
    implementation("org.postgresql:postgresql:42.3.8")  // Postgres
    runtimeOnly("org.postgresql:postgresql")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"

}

application {
    mainClass.set("MainKt")
}



publishing {
    publications {
        create<MavenPublication>("query4k") {
            groupId = "io.korpi"
            artifactId = "query4k"
            version = "0.0.1"

            pom {
                name.set("query4k")
            }
            from(components["kotlin"])
        }
    }
}

