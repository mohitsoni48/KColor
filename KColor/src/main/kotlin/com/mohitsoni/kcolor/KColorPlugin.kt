package com.mohitsoni.kcolor

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KColorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("kColor", KColorExtension::class.java)

        project.afterEvaluate {
            project.tasks.register("generateColors", GenerateColorsTask::class.java).configure {
                packageName.set(extension.packageName ?: throw NotImplementedError("packageName not added in ksp arguments"))
                sharedModule.set(extension.sharedModule ?: "shared")
                iosAppName.set(extension.iosAppName ?: "iosApp")
                projectRoot.set(project.rootDir.absolutePath.split("/$sharedModule").first())
            }

            val generateColors = project.tasks.findByName("generateColors")
            if (generateColors != null) {
                project.tasks.named("preBuild").configure {
                    dependsOn(generateColors)
                }
                project.tasks.named("embedAndSignAppleFrameworkForXcode").configure {
                    dependsOn(generateColors)
                }
            } else {
                project.logger.warn("generateColors task not found.")
            }
        }
    }
}

abstract class GenerateColorsTask : org.gradle.api.DefaultTask() {

    @Input
    val packageName = project.objects.property(String::class.java)

    @Input
    val sharedModule = project.objects.property(String::class.java)

    @Input
    val iosAppName = project.objects.property(String::class.java)

    @Input
    val projectRoot = project.objects.property(String::class.java)


    @TaskAction
    fun generate() {
        println("Color Generation Started")

        // Retrieve values
        val pkgName = packageName.get()
        val sharedMod = sharedModule.get()
        val iosApp = iosAppName.get()

        // Construct the path to colors.xml
        val xmlFilePath = "${projectRoot.get()}/$sharedMod/src/commonMain/resources/colors/colors.xml"
        val xmlFile = File(xmlFilePath)

        // Check if colors.xml exists
        if (!xmlFile.exists()) {
            throw IllegalStateException("colors.xml not found at $xmlFilePath")
        }

        // Parse the colors.xml file
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(xmlFile)
        val resources = doc.documentElement
        val colorNodes = resources.getElementsByTagName("color")

        // Generate the Android and iOS color files
        generateAndroidColors(pkgName, colorNodes, projectRoot.get(), sharedMod)
        generateIosColors(pkgName, iosApp, colorNodes, projectRoot.get(), sharedMod)
    }

    private fun generateAndroidColors(
        packageName: String,
        colorNodes: org.w3c.dom.NodeList,
        projectRoot: String,
        sharedModule: String
    ) {
        val colorDeclarations = mutableListOf<String>()
        val colorIds = mutableListOf<String>()

        for (i in 0 until colorNodes.length) {
            val node = colorNodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val value = node.textContent.trim().replace("#", "0x")
            val variableName = name.toCamelCase()

            colorDeclarations.add("val $variableName = Color($value)")
            colorIds.add("val ${variableName}: KColor = \"${node.textContent.trim()}\"")
        }

        val generatedContent = """
                |package $packageName
                |
                |import androidx.compose.ui.graphics.Color
                |
                |typealias KColor = String
                |
                |${colorDeclarations.joinToString("\n")}
                |""".trimMargin()

        val outputFilePath = "$projectRoot/$sharedModule/build/generated/colors/generatedColors.kt"
        val outputFile = File(outputFilePath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedContent)

        val generatedIds = """
                |package $packageName
                |
                |import androidx.compose.ui.graphics.Color
                |
                |object KColorRes {
                |${colorIds.joinToString("\n")}
                |}
                |""".trimMargin()

        val idFilePath = "$projectRoot/$sharedModule/build/generated/colors/generatedColorsId.kt"
        val idFile = File(idFilePath)
        idFile.parentFile.mkdirs()
        idFile.writeText(generatedIds)

        val androidGetColor = """
                |package $packageName
                |
                |import androidx.compose.ui.graphics.Color
                |fun getColor(kColor: KColor): Color {
                |   return Color(android.graphics.Color.parseColor(kColor))
                |}
                |""".trimMargin()
        val androidFilePath = "$projectRoot/$sharedModule/build/generated/android/kcolors/getColor.kt"
        val androidFile = File(androidFilePath)
        androidFile.parentFile.mkdirs()
        androidFile.writeText(androidGetColor)
    }

    private fun generateIosColors(
        packageName: String,
        iosApp: String,
        colorNodes: org.w3c.dom.NodeList,
        projectRoot: String,
        sharedModule: String
    ) {
        val outputAsset = "$projectRoot/$iosApp/$iosApp/Colors.xcassets"

        for (i in 0 until colorNodes.length) {
            val node = colorNodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val colorFolder = name.toCamelCase() + ".colorset/Contents.json"
            val outputFile = File("$outputAsset/$colorFolder")
            outputFile.parentFile.mkdirs()
            outputFile.writeText(hexToJson(node.textContent.trim()))
        }

        val iosGetColor = """
                |package $packageName
                |
                |import platform.UIKit.UIColor
                |
                |fun getColor(kColor: String): UIColor {
                |   val normalizedHex = when {
                |       kColor.length == 7 && kColor[0] == '#' -> kColor
                |       kColor.length == 9 && kColor[0] == '#' -> kColor
                |       kColor.length == 4 && kColor[0] == '#' -> {
                |           "#@{kColor[1]}@{kColor[1]}@{kColor[2]}@{kColor[2]}@{kColor[3]}@{kColor[3]}"
                |       }
                |       kColor.length == 5 && kColor[0] == '#' -> {
                |           "#@{kColor[1]}@{kColor[1]}@{kColor[2]}@{kColor[2]}@{kColor[3]}@{kColor[3]}"
                |       }
                |       else -> "#FFFFFF"
                |   }
                |
                |   val alpha = if (normalizedHex.length == 9) {
                |       normalizedHex.substring(1, 3).toInt(16) / 255.0
                |   } else {
                |       1.0
                |   }
                |   val red = normalizedHex.substring(normalizedHex.length - 6, normalizedHex.length - 4).toInt(16) / 255.0
                |   val green = normalizedHex.substring(normalizedHex.length - 4, normalizedHex.length - 2).toInt(16) / 255.0
                |   val blue = normalizedHex.substring(normalizedHex.length - 2).toInt(16) / 255.0
                |
                |   return UIColor(red = red, green = green, blue = blue, alpha = alpha)
                |}
                """.trimMargin().replace("@", "$")
        val iosFilePath = "$projectRoot/$sharedModule/build/generated/ios/kcolors/getColor.kt"
        val iosFile = File(iosFilePath)
        iosFile.parentFile.mkdirs()
        iosFile.writeText(iosGetColor)

    }

    private fun hexToJson(hex: String): String {
        val cleanedHex = hex.removePrefix("#")
        val alpha = if (cleanedHex.length == 8) cleanedHex.substring(0, 2) else "FF"
        val red = cleanedHex.substring(cleanedHex.length - 6, cleanedHex.length - 4)
        val green = cleanedHex.substring(cleanedHex.length - 4, cleanedHex.length - 2)
        val blue = cleanedHex.substring(cleanedHex.length - 2)

        val alphaDecimal = (alpha.toInt(16) / 255.0).toString()
        val redDecimal = red.toInt(16).toString()
        val greenDecimal = green.toInt(16).toString()
        val blueDecimal = blue.toInt(16).toString()

        return """
        {
          "colors": [
            {
              "color": {
                "color-space": "srgb",
                "components": {
                  "alpha": "$alphaDecimal",
                  "blue": "$blueDecimal",
                  "green": "$greenDecimal",
                  "red": "$redDecimal"
                }
              },
              "idiom": "universal"
            },
            {
              "appearances": [
                {
                  "appearance": "luminosity",
                  "value": "dark"
                }
              ],
              "color": {
                "color-space": "srgb",
                "components": {
                  "alpha": "$alphaDecimal",
                  "blue": "$blueDecimal",
                  "green": "$greenDecimal",
                  "red": "$redDecimal"
                }
              },
              "idiom": "universal"
            }
          ],
          "info": {
            "author": "xcode",
            "version": 1
          }
        }
    """.trimIndent()
    }

    private fun String.toCamelCase(): String {
        return split('_').joinToString("") { it.capitalize() }.decapitalize()
    }
}

