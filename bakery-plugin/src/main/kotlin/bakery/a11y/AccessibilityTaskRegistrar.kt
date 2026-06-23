package bakery.a11y

import bakery.BakeryExtension
import org.gradle.api.Project

object AccessibilityTaskRegistrar {

    internal fun Project.registerAccessibilityAuditTask(
        extension: BakeryExtension,
        site: bakery.SiteConfiguration
    ) {
        tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java) { task ->
            task.group = bakery.BakeryConstants.AUDIT_GROUP
            task.description = "Audit d'accessibilité WCAG/RGAA du site baké — rapport JSON/ASCII"

            val dsl = extension.a11y
            val configuredDir = dsl.auditDir.orNull?.takeIf { it.isNotBlank() }
                ?: site.bake.destDirPath.takeIf { it.isNotBlank() }
                ?: "bake"

            task.auditDir.set(
                layout.projectDirectory.dir(configuredDir)
            )

            val configuredReport = dsl.reportPath.orNull?.takeIf { it.isNotBlank() }
                ?: "build/reports/accessibility-audit.json"
            task.reportPath.set(configuredReport)

            task.conformanceLevel.set(
                dsl.conformanceLevel.orNull?.takeIf { it.isNotBlank() } ?: "AA"
            )
            task.failOnNonCompliant.set(dsl.failOnNonCompliant)

            task.mustRunAfter(bakery.BakeryConstants.BAKE_TASK)
        }
    }
}
