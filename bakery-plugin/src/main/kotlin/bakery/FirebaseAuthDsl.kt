package bakery

/**
 * DSL configuration for Firebase Auth + Comments — BKY-JB-4.
 *
 * Usage:
 * ```
 * bakery {
 *     firebaseAuth {
 *         apiKey = "AIzaSy..."
 *         authDomain = "my-app.firebaseapp.com"
 *         projectId = "my-app"
 *     }
 *     comments {
 *         enabled = true
 *         collection = "comments"
 *     }
 * }
 * ```
 */
open class FirebaseAuthDsl(
    var apiKey: String = "",
    var authDomain: String = "",
    var projectId: String = "",
)

open class CommentsDsl(
    var enabled: Boolean = false,
    var collection: String = "comments",
)