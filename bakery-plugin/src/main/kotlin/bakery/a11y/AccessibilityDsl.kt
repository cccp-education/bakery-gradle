package bakery.a11y

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL extension pour l'audit d'accessibilité.
 *
 * Usage :
 * ```
 * bakery {
 *     a11y {
 *         auditDir = "build/bake"
 *         reportPath = "build/reports/a11y.json"
 *     }
 * }
 * ```
 */
open class AccessibilityDsl @Inject constructor(objects: ObjectFactory) {
    val auditDir: Property<String> = objects.property(String::class.java)
    val reportPath: Property<String> = objects.property(String::class.java)
    val conformanceLevel: Property<String> = objects.property(String::class.java)

    init {
        auditDir.convention("")
        reportPath.convention("")
        conformanceLevel.convention("")
    }
}
