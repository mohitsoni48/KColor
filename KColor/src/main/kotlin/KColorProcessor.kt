import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KColorProcessor(
    private val environment: SymbolProcessorEnvironment,
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("###KSP###: Started")
        val allFiles = resolver.getAllFiles().map { it.fileName }
        logger.warn(allFiles.toList().toString())
        if (invoked) {
            return emptyList()
        }
        invoked = true

        val packageName = environment.options["packageName"]

        if (packageName == null) {
            logger.error("packageName not added in ksp arguments")
            throw NotImplementedError("packageName not added in ksp arguments")
        }

        val sharedModuleName = environment.options["sharedModuleName"] ?: "shared"
        val iosApp = environment.options["iosAppName"] ?: "iosApp"
        val projectRoot = resolver.getAllFiles().find { it.filePath.contains("/$sharedModuleName/") }!!.filePath.split("/$sharedModuleName/").first()
        val relativePath = "$sharedModuleName/src/commonMain/resources/colors/colors.xml"

        val xmlFilePath = "$projectRoot/$relativePath"

        val xmlFile = File(xmlFilePath)
        if (!xmlFile.exists()) {
            logger.error("colors.xml not found at $xmlFilePath")
            return emptyList()
        }

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.parse(xmlFile)
        val resources = doc.documentElement

        val stringNodes = resources.getElementsByTagName("color")

        generateAndroidColors(stringNodes, packageName, projectRoot, sharedModuleName)
        generateiosColors(iosApp, stringNodes, packageName, projectRoot, sharedModuleName)

        return emptyList()
    }

    private fun generateAndroidColors(
        stringNodes: NodeList,
        packageName: String?,
        projectRoot: String,
        sharedModuleName: String
    ) {
        val colorDeclarations = mutableListOf<String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val value = node.textContent.trim().replace("#", "0x")
            val variableName = name.toCamelCase()

            colorDeclarations.add("val $variableName = Color($value)")
        }

        val generatedContent = """
                |package $packageName
                |
                |import androidx.compose.ui.graphics.Color
                |
                |${colorDeclarations.joinToString("\n")}
                |""".trimMargin()

        val outputFilePath =
            "$projectRoot/$sharedModuleName/build/generated/colors/generatedcolors.kt"
        val outputFile = File(outputFilePath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedContent)

        logger.info("Generated colors.kt at $outputFilePath")
    }

    private fun generateiosColors(
        iosApp: String,
        stringNodes: NodeList,
        packageName: String?,
        projectRoot: String,
        sharedModuleName: String
    ) {

        val outputAsset = "$projectRoot/$iosApp/$iosApp/Colors.xcassets"
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val colorFolder = name.transformToCapitalCamelCase() + ".colorset/Contents.json"
            val outputFile = File("$outputAsset/$colorFolder")
            outputFile.parentFile.mkdirs()
            outputFile.writeText(hexToJson(node.textContent.trim()))
        }

    }

    fun hexToJson(hex: String): String {
        // Ensure hex is in the format #RRGGBB or #AARRGGBB
        val cleanedHex = hex.removePrefix("#")
        val alpha = if (cleanedHex.length == 8) cleanedHex.substring(0, 2) else "FF"
        val red = cleanedHex.substring(cleanedHex.length - 6, cleanedHex.length - 4)
        val green = cleanedHex.substring(cleanedHex.length - 4, cleanedHex.length - 2)
        val blue = cleanedHex.substring(cleanedHex.length - 2, cleanedHex.length)

        // Convert hex values to decimal
        val alphaDecimal = (alpha.toInt(16) / 255.0).toString()
        val redDecimal = Integer.parseInt(red, 16).toString()
        val greenDecimal = Integer.parseInt(green, 16).toString()
        val blueDecimal = Integer.parseInt(blue, 16).toString()

        // Construct JSON manually
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
        return this.split('_').joinToString("") { it.capitalize() }.decapitalize()
    }

    fun String.transformToCapitalCamelCase(): String {
        return this.split('_')
            .joinToString("") { it.capitalize() }
    }

}

class KColorProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KColorProcessor(environment, environment.codeGenerator, environment.logger)
    }
}
