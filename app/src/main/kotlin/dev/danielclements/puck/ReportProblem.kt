package dev.danielclements.puck

import java.net.URLEncoder

/** Where bug reports, the privacy policy and the source live. */
object ProjectLinks {
    const val REPO = "https://github.com/iamclements/puck"
    const val PRIVACY = "$REPO/blob/main/PRIVACY.md"
    const val LICENSES = "$REPO/blob/main/THIRD_PARTY_NOTICES.md"
}

/**
 * Builds the pre-filled GitHub issue behind Settings → Report a problem.
 *
 * Only facts useful for debugging go in: app version, Android version, phone
 * model and the Apple TV *models* in use. No names, addresses or pairing
 * data, and nothing is sent anywhere until the user submits the issue.
 */
object ReportProblem {
    fun url(
        appVersion: String,
        versionCode: Int,
        androidRelease: String,
        sdk: Int,
        phoneModel: String,
        tvModels: Collection<String>,
    ): String {
        val tvs = tvModels.filter { it.isNotBlank() }.distinct().ifEmpty { listOf("unknown") }
        val body = buildString {
            appendLine("**What happened?**")
            appendLine()
            appendLine()
            appendLine("**What did you expect?**")
            appendLine()
            appendLine()
            appendLine("**Steps to reproduce**")
            appendLine()
            appendLine()
            appendLine("---")
            appendLine("Puck $appVersion ($versionCode)")
            appendLine("Android $androidRelease (API $sdk), $phoneModel")
            append("Apple TV: ${tvs.joinToString(", ")}")
        }
        return "${ProjectLinks.REPO}/issues/new?title=${enc("")}&body=${enc(body)}"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
