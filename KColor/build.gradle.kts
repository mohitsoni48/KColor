import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.vanniktech.maven.publish") version "0.28.0"

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

tasks.register<JavaCompile>("GenerateKColor") {
    group = "kotlin"
    description = "Generate colors for android"
    source = fileTree("src/main/kotlin")
    classpath = configurations.kotlinCompilerClasspath.get()
    destinationDirectory = file("build/generated/ksp/android/androidMain/kotlin")
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

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = "io.github.mohitsoni48",
        artifactId = "KColor",
        version = "1.0.0.alpha2"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KColor")
        description.set("A library to share color between commonMain and ios swift")
        inceptionYear.set("2024")
        url.set("https://github.com/mohitsoni48/KColor")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        // Specify developers information
        developers {
            developer {
                id.set("mohitsoni48")
                name.set("Mohit Soni")
                email.set("mohitsoni48@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/mohitsoni48/KColor")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}

//kColor {
//    packageName = "com.aistro.magha"
//    sharedModule = "shared"
//}