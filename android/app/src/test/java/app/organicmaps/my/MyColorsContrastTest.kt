package app.organicmaps.my

import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA contrast of the Malaysia Nav token pairs in both themes (values/ and values-night/my_colors.xml):
 * 4.5:1 for text, 3:1 for large text (>= 18sp bold) and icons. Translucent tokens are composited over my_panel.
 */
class MyColorsContrastTest {
    private data class Case(val fg: String, val bg: String, val minRatio: Double, val what: String)

    private val pairs = listOf(
        Case("my_ink", "my_panel", TEXT, "chip text, sheet"),
        Case("my_muted", "my_panel", TEXT, "ETA subtitle"),
        Case("my_muted", "my_panel_2", TEXT, "toll label"),
        Case("my_accent_text", "my_panel_2", TEXT, "toll amount"),
        Case("my_ok", "my_panel", LARGE, "ETA"),
        Case("my_danger", "my_panel", LARGE, "speed over limit"),
        Case("my_on_banner", "my_turn_banner_start", TEXT, "turn street on gradient start"),
        Case("my_on_banner", "my_turn_banner_end", TEXT, "turn street on gradient end"),
        Case("my_on_banner_muted", "my_turn_banner_end", TEXT, "'Then' label"),
        Case("my_on_banner", "my_danger", TEXT, "End button, speed camera pill"),
        Case("my_ink", "my_pill_bg", TEXT, "speed value"),
        Case("my_muted", "my_pill_bg", TEXT, "speed units"),
        Case("my_lane_inactive", "my_pill_bg", LARGE, "inactive lane"),
        Case("my_ok", "my_pill_bg", LARGE, "active lane"),
        Case("my_route", "my_panel", LARGE, "voice icon on"),
        Case("my_route", "my_track", LARGE, "progress fill on track"),
    )

    @Test
    fun dayTokensMeetWcagAa() = check("src/main/res/values/my_colors.xml")

    @Test
    fun nightTokensMeetWcagAa() = check("src/main/res/values-night/my_colors.xml")

    private fun check(path: String) {
        val colors = Regex("""<color name="(\w+)">#([0-9A-Fa-f]{8})</color>""")
            .findAll(File(path).readText())
            .associate { it.groupValues[1] to it.groupValues[2].toLong(16) }
        val panel = rgb(colors.getValue("my_panel"))
        for (pair in pairs) {
            val bg = over(colors.getValue(pair.bg), panel)
            val fg = over(colors.getValue(pair.fg), bg)
            val ratio = contrast(fg, bg)
            assertTrue(
                "$path: ${pair.fg} on ${pair.bg} (${pair.what}) is ${"%.2f".format(ratio)}:1, needs ${pair.minRatio}:1",
                ratio >= pair.minRatio,
            )
        }
    }

    private fun rgb(argb: Long) = DoubleArray(3) { ((argb shr (16 - 8 * it)) and 0xFF) / 255.0 }

    /** Alpha-composites an ARGB colour over an opaque background. */
    private fun over(argb: Long, bg: DoubleArray): DoubleArray {
        val alpha = ((argb shr 24) and 0xFF) / 255.0
        val c = rgb(argb)
        return DoubleArray(3) { alpha * c[it] + (1 - alpha) * bg[it] }
    }

    private fun luminance(c: DoubleArray): Double {
        val lin = c.map { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]
    }

    private fun contrast(a: DoubleArray, b: DoubleArray): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private companion object {
        const val TEXT = 4.5
        const val LARGE = 3.0
    }
}
