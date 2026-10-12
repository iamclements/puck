package dev.danielclements.puck

import java.net.URLDecoder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportProblemTest {

    private fun build(tvs: List<String> = listOf("AppleTV14,1", "AppleTV11,1", "AppleTV14,1")) = ReportProblem.url(
        appVersion = "1.3.2", versionCode = 6, androidRelease = "16", sdk = 36,
        phoneModel = "Google Pixel 9", tvModels = tvs,
    )

    private fun body(url: String) =
        URLDecoder.decode(url.substringAfter("body="), "UTF-8")

    @Test fun `points at the issue form`() =
        assertTrue(build().startsWith("https://github.com/iamclements/puck/issues/new?"))

    @Test fun `carries versions and de-duplicated TV models`() {
        val b = body(build())
        assertTrue(b.contains("Puck 1.3.2 (6)"))
        assertTrue(b.contains("Android 16 (API 36), Google Pixel 9"))
        assertTrue(b.contains("Apple TV: AppleTV14,1, AppleTV11,1"))
    }

    @Test fun `the url has no raw spaces or newlines`() {
        val url = build()
        assertFalse(url.contains(' '))
        assertFalse(url.contains('\n'))
    }

    @Test fun `no paired TVs reads as unknown`() =
        assertTrue(body(build(emptyList())).contains("Apple TV: unknown"))
}
