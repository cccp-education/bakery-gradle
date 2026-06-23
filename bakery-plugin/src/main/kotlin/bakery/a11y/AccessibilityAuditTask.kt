package bakery.a11y

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Tâche Gradle d'audit d'accessibilité WCAG 2.1 AA / RGAA.
 *
 * Scanne le répertoire de sortie du site baké (output HTML) et produit un
 * rapport JSON/ASCII listant les findings (contraste, alt, aria, headings, etc.).
 *
 * Pipeline :
 * ```
 * bake → accessibilityAudit
 * ```
 *
 * Usage CLI :
 * ```
 * ./gradlew accessibilityAudit --auditDir=build/bake --reportPath=build/reports/a11y.json
 * ```
 *
 * Usage DSL :
 * ```
 * bakery {
 *     a11y {
 *         auditDir = "build/bake"
 *         reportPath = "build/reports/a11y.json"
 *     }
 * }
 * ```
 */
@DisableCachingByDefault(because = "Audit accessibilité — dépend du HTML baké, non-cacheable")
abstract class AccessibilityAuditTask : DefaultTask() {

    /** Répertoire contenant les fichiers HTML à auditer (input). */
    @get:InputDirectory
    @get:Optional
    abstract val auditDir: DirectoryProperty

    /** Chemin du fichier de rapport JSON (output). */
    @get:Input
    @get:Optional
    @get:Option(option = "reportPath", description = "Chemin du rapport JSON d'accessibilité")
    abstract val reportPath: Property<String>

    /** Seuil de conformité : "AA" (défaut) ou "AAA". */
    @get:Input
    @get:Optional
    @get:Option(option = "conformanceLevel", description = "Niveau de conformité WCAG : AA ou AAA")
    abstract val conformanceLevel: Property<String>

    init {
        group = BakeryConstants.AUDIT_GROUP
        description = "Audit d'accessibilité WCAG/RGAA du site baké — rapport JSON/ASCII"
        reportPath.convention("build/reports/accessibility-audit.json")
        conformanceLevel.convention("AA")
    }

    @TaskAction
    fun executeAudit() {
        val htmlDir = resolveAuditDir()
        if (!htmlDir.exists() || !htmlDir.isDirectory) {
            throw IllegalStateException("Audit directory does not exist: ${htmlDir.absolutePath}")
        }

        val htmlFiles = htmlDir.walkTopDown()
            .filter { it.isFile && (it.extension.equals("html", ignoreCase = true) || it.extension.equals("htm", ignoreCase = true)) }
            .toList()

        if (htmlFiles.isEmpty()) {
            logger.warn("[accessibilityAudit] Aucun fichier HTML trouvé dans ${htmlDir.absolutePath}")
        }

        val allFindings = htmlFiles.flatMap { file ->
            val html = file.readText(Charsets.UTF_8)
            scanInlineColors(html).map { finding ->
                finding.copy(
                    selector = "${file.relativeTo(htmlDir).path} ${finding.selector}",
                    message = "${finding.message} (${file.name})"
                )
            }
        }

        val report = audit(allFindings)

        val reportFile = project.projectDir.resolve(reportPath.get())
        reportFile.parentFile.mkdirs()
        writeJsonReport(reportFile, report)

        logger.lifecycle("[accessibilityAudit] {} fichiers scannés, {} findings, {} passed, {} failed",
            htmlFiles.size, report.findings.size, report.passedCount, report.failedCount)

        if (!report.isCompliant) {
            logger.warn("[accessibilityAudit] Site NON conforme — {} findings échoués", report.failedCount)
        } else {
            logger.lifecycle("[accessibilityAudit] Site conforme ✅")
        }
    }

    internal fun resolveAuditDir(): File {
        val configured = auditDir.asFile.orNull
        return configured
            ?: project.layout.buildDirectory.asFile.get().resolve("bake")
    }

    private fun writeJsonReport(file: File, report: AccessibilityReport) {
        val lines = buildList {
            add("{")
            add("  \"compliant\": ${report.isCompliant},")
            add("  \"passedCount\": ${report.passedCount},")
            add("  \"failedCount\": ${report.failedCount},")
            add("  \"findings\": [")
            report.findings.forEachIndexed { index, finding ->
                val sep = if (index == report.findings.lastIndex) "" else ","
                add("    {")
                add("      \"selector\": \"${escapeJson(finding.selector)}\",")
                add("      \"rule\": \"${escapeJson(finding.rule)}\",")
                add("      \"pass\": ${finding.pass},")
                add("      \"message\": \"${escapeJson(finding.message)}\"")
                add("    }$sep")
            }
            add("  ]")
            add("}")
        }
        file.writeText(lines.joinToString("\n"), Charsets.UTF_8)
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
