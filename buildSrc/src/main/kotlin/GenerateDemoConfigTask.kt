import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.Properties

abstract class GenerateDemoConfigTask : DefaultTask() {
    @get:Input
    abstract val showCredentialsFromEnvironment: Property<String>

    @get:Input
    abstract val showCredentialsFromGradle: Property<String>

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val show = parseBoolean(
            firstPresent(
                showCredentialsFromEnvironment.get(),
                localProperty("DEMO_SHOW_CREDENTIALS"),
                showCredentialsFromGradle.get(),
            ),
        )
        val output = outputDirectory.get().asFile.resolve("com/contractproof/demo/DemoLocalConfig.kt")
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.contractproof.demo

            internal object DemoLocalConfig {
                const val showCredentialsHint: Boolean = $show
            }
            """.trimIndent() + "\n",
        )
    }

    private fun localProperty(name: String): String {
        val file = localPropertiesFile.asFile.orNull ?: return ""
        if (!file.exists()) return ""
        val properties = Properties()
        file.inputStream().use(properties::load)
        return properties.getProperty(name).orEmpty()
    }

    private fun firstPresent(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun parseBoolean(raw: String): Boolean {
        return when (raw.trim().lowercase()) {
            "1", "true", "yes", "on" -> true
            else -> false
        }
    }
}
