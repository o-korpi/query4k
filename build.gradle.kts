import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotestVersion = "5.6"

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("maven-publish")
    application
}

group = "io.korpi"
version = "0.3.1"

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

    // Testing
    implementation("com.zaxxer:HikariCP:5.0.1")
    testImplementation(kotlin("test"))
    implementation("io.kotest:kotest-runner-junit5:kotestVersion")
    implementation("io.kotest:kotest-assertions-core:kotestVersion")
    implementation("io.kotest:kotest-assertions-property:kotestVersion")
    implementation("org.postgresql:postgresql:42.3.8")
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
        create<MavenPublication>("maven") {
            groupId = "io.korpi"
            artifactId = "query4k"
            version = "0.3.1"

            pom {
                name.set("query4k")
            }
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

