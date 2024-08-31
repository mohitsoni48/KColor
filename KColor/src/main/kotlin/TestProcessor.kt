import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import org.w3c.dom.Document
import java.io.File
import java.io.OutputStreamWriter
import javax.xml.parsers.DocumentBuilderFactory

class TestProcessor(private val environment: SymbolProcessorEnvironment, val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("###KSP###: Started")
        val allFiles = resolver.getAllFiles().map { it.fileName }
        logger.warn(allFiles.toList().toString())
        if (invoked) {
            return emptyList()
        }
        invoked = true
        invoked = true


        // Specify the path to the XML file
        val projectRoot = resolver.getAllFiles().find { it.filePath.contains("/shared/") }!!.filePath.split("/shared/").first()
        val relativePath = "shared/src/commonMain/resources/colors/colors.xml"

        // Construct the full path
        val xmlFilePath = "$projectRoot/$relativePath"

        // Read the XML file (assuming it exists)
        val xmlFile = File(xmlFilePath)
        if (!xmlFile.exists()) {
            logger.error("colors.xml not found at $xmlFilePath")
            return emptyList()
        }

        // Example: Extract package name from any KSClassDeclaration
        val packageName = "com.mohitsoni.kcolorsample"
        logger.warn("###KSP###: Package name: $packageName")
        // Define the output file
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.parse(xmlFile)
        val resources = doc.documentElement

        // Prepare the content for the generated Kotlin file
        val stringNodes = resources.getElementsByTagName("color")
        val colorDeclarations = mutableListOf<String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val value = node.textContent.trim().replace("#", "0x")
            val variableName = name.toCamelCase()

            colorDeclarations.add("val $variableName = Color($value)")
        }

        // Generate the content for generatedcolors.kt
        val generatedContent = """
            |package $packageName
            |
            |import androidx.compose.ui.graphics.Color
            |
            |${colorDeclarations.joinToString("\n")}
            |""".trimMargin()

        // Define the output file
        val outputFilePath = "$projectRoot/shared/src/commonMain/kotlin/${packageName.replace('.', '/')}/generatedcolors.kt"
        val outputFile = File(outputFilePath)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedContent)

        logger.info("Generated colors.kt at $outputFilePath")

        return emptyList()
    }

    private fun String.toCamelCase(): String {
        return this.split('_').joinToString("") { it.capitalize() }.decapitalize()
    }

}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TestProcessor(environment, environment.codeGenerator, environment.logger)
    }
}
