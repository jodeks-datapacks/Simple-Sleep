plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
    id("run-hytale")
}

group = findProperty("pluginGroup") as String? ?: "com.jodek"
version = findProperty("pluginVersion") as String? ?: "1.0.0"
description = findProperty("pluginDescription") as String? ?: "Configure the percentage of players that need to sleep to skip the night"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Hytale Server API (provided by server at runtime)
    compileOnly(files("libs/HytaleServer.jar"))
    
    // Common dependencies (will be bundled in JAR)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.1.0")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure server testing
runHytale {
    // Use local HytaleServer.jar from libs/ directory
    // Place your HytaleServer.jar in the libs/ folder
    jarUrl = "file:///${project.projectDir.absolutePath}/libs/HytaleServer.jar".replace("\\", "/")

    // Path to Assets.zip (required for server to start)
    // Copy Assets.zip from your Hytale installation or download it with Hytale Downloader CLI
    // Example locations:
    //   - From Hytale installation: %APPDATA%\Roaming\Hytale\Assets.zip
    //   - Place in project: libs/Assets.zip
    assetsPath = "${project.projectDir.absolutePath}/libs/Assets.zip".replace("\\", "/")
}

tasks {
    // Configure Java compilation
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }
    
    // Configure resource processing
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        
        // Replace placeholders in manifest.json
        val props = mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        
        filesMatching("manifest.json") {
            expand(props)
        }
    }
    
    // Configure ShadowJar (bundle dependencies)
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        
        // Relocate dependencies to avoid conflicts
        relocate("com.google.gson", "com.simplesleep.libs.gson")
        
        // Minimize JAR size (removes unused classes)
        minimize()
    }
    
    // Configure tests
    test {
        useJUnitPlatform()
    }
    
    // Make build depend on shadowJar
    build {
        dependsOn(shadowJar)
    }
}

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
