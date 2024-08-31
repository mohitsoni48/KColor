plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("com.squareup:javapoet:1.13.0")
                implementation(libs.symbol.processing)
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}

tasks.register<JavaCompile>("generateKotlinCodeAndroid") {
    group = "kotlin"
    description = "Generate Kotlin code with KSP for Android"
    source = fileTree("src/main/kotlin")
    classpath = configurations.kotlinCompilerClasspath.get()
    destinationDirectory = file("androidApp/build/generated/ksp/android/androidMain/kotlin")
    // Add other necessary configurations
}