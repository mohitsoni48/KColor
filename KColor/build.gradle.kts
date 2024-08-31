plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id ("maven-publish")
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.symbol.processing)
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}

tasks.register<JavaCompile>("generateKotlinCodeAndroid") {
    group = "kotlin"
    description = "Generate colors for android"
    source = fileTree("src/main/kotlin")
    classpath = configurations.kotlinCompilerClasspath.get()
    destinationDirectory = file("androidApp/build/generated/ksp/android/androidMain/kotlin")
}

open class KColorExtension {
    var packageName: String? = null
    var sharedModule: String = "shared"
}

class KColorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("kColor", KColorExtension::class.java)

        project.afterEvaluate {
            val packageName = extension.packageName
            val sharedModule = extension.sharedModule

            // Use these properties as needed, e.g., pass them to your KSP processor
            println("Package Name: $packageName")
            println("Shared Module: $sharedModule")
        }
    }
}

apply<KColorPlugin>()

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "com.github.mohitsoni48"
            artifactId = "kcolor-plugin"
            version = "1.0.0"

//            afterEvaluate {
//                from(components["java"])
//            }
        }
    }
    repositories {
        maven {
            url = uri("file://${layout.buildDirectory}/repo")
        }
    }
}

//kColor {
//    packageName = "com.aistro.magha"
//    sharedModule = "shared"
//}