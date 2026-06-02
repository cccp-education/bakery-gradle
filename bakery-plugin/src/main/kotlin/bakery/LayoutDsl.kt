package bakery

/**
 * DSL configuration for Layout system — BKY-JB-7.
 *
 * Usage:
 * ```
 * bakery {
 *     layout {
 *         layoutType = LayoutType.SIDEBAR_LEFT
 *     }
 * }
 * ```
 */
enum class LayoutType { FULL_WIDTH, SIDEBAR_LEFT, SIDEBAR_RIGHT, CENTERED }

open class LayoutDsl(
    var layoutType: LayoutType = LayoutType.FULL_WIDTH,
)
