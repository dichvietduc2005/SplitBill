import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "3.0.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

group = "com.splitbill"
version = "0.0.1"



kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

application {
    mainClass.set("com.splitbill.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Auth & Security
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("org.mindrot:jbcrypt:0.4")

    // Exposed & PostgreSQL
    implementation("org.jetbrains.exposed:exposed-core:0.50.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
