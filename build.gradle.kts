import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    // id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.8.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
    kotlin("jvm") version "1.7.21"
}

group = "chen.yyds"
version = "1.51"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set("PC") // Target IDE Platform
    intellij.updateSinceUntilBuild.set(false)
    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.17.4")
    }
}

configurations.all {
    resolutionStrategy {
        // 修改 gradle不自动处理版本冲突
        // failOnVersionConflict()
        // preferProjectModules()
        force("org.slf4j:slf4j-api:1.7.20")
        // cacheChangingModulesFor(0, "seconds")
    }
}

val ktorVersion = "2.3.10"

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion") {
        exclude(group="org.slf4j")
    }
    implementation("io.ktor:ktor-client-java:$ktorVersion")  {
        exclude(group="org.slf4j")
    }
    implementation("io.ktor:ktor-client-websockets-jvm:$ktorVersion") {
        exclude(group="org.slf4j")
    }
    implementation("io.ktor:ktor-client-logging:$ktorVersion") {
        exclude(group="org.slf4j")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktorVersion")  {
        exclude(group="org.slf4j")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")  {
        exclude(group="org.slf4j")
    }
    implementation(kotlin("stdlib-jdk8"))

}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}