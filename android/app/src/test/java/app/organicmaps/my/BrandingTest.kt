package app.organicmaps.my

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** User-visible strings must say Sampai; the attribution lives on the licences screen only. */
class BrandingTest {
    @Test
    fun noStringShowsTheUpstreamBrandOrItsLinks() {
        val leaks = File("../../data/strings/strings.txt").readLines()
            .filter { it.contains(" = ") && !it.startsWith("comment") }
            .filter { line -> UPSTREAM_BRAND.containsMatchIn(line) }
        assertEquals(emptyList<String>(), leaks)
    }

    private companion object {
        val UPSTREAM_BRAND = Regex("Organic ?Maps|organicmaps\\.app|omaps\\.app", RegexOption.IGNORE_CASE)
    }
}
