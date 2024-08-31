import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import java.io.File
import java.io.OutputStreamWriter

class TestProcessor(private val environment: SymbolProcessorEnvironment, val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("###KSP###: Started")
        println("###KSP###: started")
        val allFiles = resolver.getAllFiles().map { it.fileName }
        logger.warn(allFiles.toList().toString())
        if (invoked) {
            return emptyList()
        }
        invoked = true
        println("###KSP###: Invoked")
        var outputPath = ""
        var packageName = "com.mohitsoni.kcolorsample"
        var filename = "Sample"
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "", filename, "kt").use { output ->

            outputPath =
                "/Users/mohitsoni/AndroidStudioProjects/KColor/androidApp/src/main/java/${
                    packageName.replace(
                        ".",
                        "/"
                    )
                }/android/generated"
            val outputFile = File(outputPath, "${filename}Impl.kt")

            outputFile.parentFile.mkdirs()
            val generatedOutput = """
            |package $packageName.android.generated
            |
            |import $packageName.sample
            |
            |data class SampleImpl(
            |    var id: Long = 0,   
            |)
            |""".trimMargin()
            outputFile.writeText(generatedOutput)

        }
//        generateAndroidEntities(resolver)
//        generateIosEntities(resolver)
        return emptyList()
    }

    private fun generateAndroidEntities(resolver: Resolver) {
        val annotatedInterfaces =
            resolver.getSymbolsWithAnnotation("KColor", true)
        val moduleImports = ArrayList<String>()
        val propertyStringsMap = HashMap<String, String>()
        val propertyAssignmentStringsMap = HashMap<String, String>()

        var outputPath = ""
        var packageName = ""
        annotatedInterfaces.forEach { annotated ->
            if (annotated !is KSClassDeclaration) {
                logger.error("@KColor annotation applied to a non-interface: $annotated")
                return
            }
            println("###KSP###: Passed")
            packageName = annotated.packageName.asString()
            val filename = annotated.simpleName.asString()
            codeGenerator.createNewFile(Dependencies.ALL_FILES, "", filename, "kt").use { output ->

                outputPath =
                    "/Users/mohitsoni/AndroidStudioProjects/KColor/androidApp/src/main/java/${
                        packageName.replace(
                            ".",
                            "/"
                        )
                    }/android/generated"
                val outputFile = File(outputPath, "${filename}Impl.kt")

                outputFile.parentFile.mkdirs()
                val generatedOutput = generateImplementationCodeAndroid(annotated)
                outputFile.writeText(generatedOutput)

                moduleImports.add("|import com.example.mykmmapplication.$filename")
            }

            val properties = annotated.getAllProperties()
            val propertiesString = properties.joinToString(separator = ",\n") { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve().declaration.qualifiedName!!.asString()
                "$propertyName: $propertyType"
            }

            val propertiesAssignmentString =
                properties.joinToString(separator = ",\n") { property ->
                    val propertyName = property.simpleName.asString()
                    "$propertyName = $propertyName"
                }
            propertyStringsMap[filename] = propertiesString
            propertyAssignmentStringsMap[filename] = propertiesAssignmentString

        }
        generateModulesFile(
            outputPath,
            annotatedInterfaces,
            propertyStringsMap,
            propertyAssignmentStringsMap,
            packageName,
            moduleImports
        )
    }

    private fun generateIosEntities(resolver: Resolver) {
        val annotatedInterfaces =
            resolver.getSymbolsWithAnnotation("com.example.mykmmapplication.ObjectBoxEntity", true)

        var outputPath = ""
        var packageName = ""

        annotatedInterfaces.forEach { annotated ->
            if (annotated !is KSClassDeclaration) {
                logger.error("@ObjectBoxEntity annotation applied to a non-interface: $annotated")
                return
            }
            packageName = annotated.packageName.asString()
            val filename = annotated.simpleName.asString()
            outputPath =
                "/Users/mohitsoni/AndroidStudioProjects/MyKMMApplication/iosApp/generated/objectBoxEntities"
            val outputFile = File(outputPath, "${filename}Impl.swift")

            outputFile.parentFile.mkdirs()
            outputFile.writeText(generateImplementationCodeIos(annotated))
        }
    }

    private fun generateModulesFile(
        outputPath: String,
        annotatedInterfaces: Sequence<KSAnnotated>,
        propertyStringsMap: HashMap<String, String>,
        propertyAssignmentStringsMap: HashMap<String, String>,
        packageName: String,
        moduleImports: ArrayList<String>
    ) {
        val moduleFile = File(outputPath, "EntityModules.kt")
        moduleFile.parentFile.mkdirs()

        val factories = ArrayList<String>()
        annotatedInterfaces.iterator().forEach { annotated ->
            if (annotated !is KSClassDeclaration) {
                logger.error("@ObjectBoxEntity annotation applied to a non-interface: $annotated")
                return
            }

            val filename = annotated.simpleName.asString()
            val factoryString = """
                    module {
                    |    factory<(${propertyStringsMap[filename]}) -> ObjectBoxObject<out $filename>> {
                    |        { ${propertyStringsMap[filename]!!} ->
                    |           ObjectBoxObjectAndroid(${filename}Impl(${propertyAssignmentStringsMap[filename]})) 
                    |        }
                    |    }
                    |}
                """.trimIndent()

            factories.add(factoryString)
        }

        moduleFile.writeText(
            """
                    |package $packageName.android.generated
                    |
                    |import org.koin.dsl.module
                    |import com.example.mykmmapplication.ObjectBoxObject
                    |import com.example.mykmmapplication.android.ObjectBoxObjectAndroid
                    ${moduleImports.joinToString("\n")}
                    |
                    |val entitiesModule = listOf(${factories.joinToString(",\n\n")})                
                    """.trimMargin()
        )
    }

    private fun generateImplementationCodeAndroid(interfaceSymbol: KSClassDeclaration): String {
        val packageName = interfaceSymbol.packageName.asString()
        val className = interfaceSymbol.simpleName.asString()

        val properties = interfaceSymbol.getAllProperties()
        val propertiesString = properties.joinToString(separator = ",\n") { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.resolve().declaration.qualifiedName!!.asString()
            "override var $propertyName: $propertyType = ${getDefaultValueForPropertyType(property)}"
        }

        return """
            |package $packageName.android.generated
            |
            |import $packageName.$className
            |
            |data class ${className}Impl(
            |    var id: Long = 0,
            |    $propertiesString
            |): $className
            |""".trimMargin()
    }

    private fun generateImplementationCodeIos(interfaceSymbol: KSClassDeclaration): String {
        val packageName = interfaceSymbol.packageName.asString()
        val className = interfaceSymbol.simpleName.asString()

        val properties = interfaceSymbol.getAllProperties()
        val propertiesString = properties.joinToString(separator = "\n") { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.resolve().declaration.qualifiedName!!.toIOSQualifiers()
            "var $propertyName: $propertyType = ${getDefaultValueForPropertyType(property)}"
        }

        val assignmentString = properties.joinToString(separator = "\n") { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.resolve().declaration.qualifiedName!!.toIOSQualifiers()
            "self.$propertyName = ${getDefaultValueForPropertyType(property)}"
        }


        val parametersString = properties.joinToString(separator = ",\n") { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.resolve().declaration.qualifiedName!!.toIOSQualifiers()
            "$propertyName: $propertyType"
        }


        val convenienceString = properties.joinToString(separator = "\n") { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.resolve().declaration.qualifiedName!!.toIOSQualifiers()
            "self.$propertyName = $propertyName"
        }
        return """
            |import shared
            |
            |// objectbox:entity
            |class ${className}Impl : $className{
            |    var id: Id = 0
            |    $propertiesString
            |    
            |    required init() {
            |       $assignmentString
            |    }
            |    
            |    convenience init($parametersString){
            |       self.init()
            |       $convenienceString
            |    }
            |}
            |""".trimMargin()
    }
}


private fun getDefaultValueForPropertyType(property: KSPropertyDeclaration): String {
    val typeName = property.type.resolve().declaration.qualifiedName?.asString()
    return when (typeName) {
        "kotlin.String" -> "\"\""
        "kotlin.Int", "kotlin.Long" -> "0"
        else -> "null" // Default value for other types
    }
}

private fun KSName.toIOSQualifiers(): String {
    return when(this.asString()) {
        "kotlin.String" -> "String"
        "kotlin.Long" -> "Int64"
        "kotlin.Int" -> "Int32"
        else -> this.asString()
    }
}

class ClassVisitor : KSTopDownVisitor<OutputStreamWriter, Unit>() {
    override fun defaultHandler(node: KSNode, data: OutputStreamWriter) {
    }

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: OutputStreamWriter
    ) {
        super.visitClassDeclaration(classDeclaration, data)
        val symbolName = classDeclaration.simpleName.asString().lowercase()
        data.write("    val $symbolName = true\n")
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TestProcessor(environment, environment.codeGenerator, environment.logger)
    }
}
