package com.example.pocketbill.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 AA contrast for the colour tokens (UX_SPEC.md sections 7.1 and 8.1):
 * text needs at least 4.5:1, borders and meaningful icons at least 3:1.
 * Runs whenever a colour token changes, as the spec requires.
 */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertContrast(label: String, fg: Color, bg: Color, minimum: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$label: %.2f:1, needs %.1f:1".format(ratio, minimum), ratio >= minimum)
    }

    private fun checkScheme(name: String, c: ColorScheme) {
        val text = 4.5
        assertContrast("$name onSurface/surface", c.onSurface, c.surface, text)
        assertContrast("$name onBackground/background", c.onBackground, c.background, text)
        assertContrast("$name onSurfaceVariant/surface", c.onSurfaceVariant, c.surface, text)
        assertContrast("$name onSurfaceVariant/background", c.onSurfaceVariant, c.background, text)
        assertContrast("$name onSurface/surfaceContainer (bottom bar)", c.onSurface, c.surfaceContainer, text)
        assertContrast("$name onSurfaceVariant/surfaceContainer", c.onSurfaceVariant, c.surfaceContainer, text)
        assertContrast("$name primary/background (links)", c.primary, c.background, text)
        assertContrast("$name primary/surface", c.primary, c.surface, text)
        assertContrast("$name onPrimary/primary", c.onPrimary, c.primary, text)
        assertContrast("$name onPrimaryContainer/primaryContainer", c.onPrimaryContainer, c.primaryContainer, text)
        assertContrast("$name onSecondaryContainer/secondaryContainer (selected tab)", c.onSecondaryContainer, c.secondaryContainer, text)
        assertContrast("$name onTertiaryContainer/tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer, text)
        assertContrast("$name error/surface", c.error, c.surface, text)
        assertContrast("$name onErrorContainer/errorContainer", c.onErrorContainer, c.errorContainer, text)
        assertContrast("$name inverseOnSurface/inverseSurface (snackbar)", c.inverseOnSurface, c.inverseSurface, text)
        assertContrast("$name inversePrimary/inverseSurface (snackbar action)", c.inversePrimary, c.inverseSurface, text)
        assertContrast("$name outline/surface (field borders)", c.outline, c.surface, 3.0)
        assertContrast("$name outline/background", c.outline, c.background, 3.0)
    }

    private fun checkStatus(name: String, s: StatusColors, c: ColorScheme) {
        for ((label, status) in listOf("paid" to s.paid, "unpaid" to s.unpaid, "overdue" to s.overdue, "void" to s.void)) {
            assertContrast("$name status.$label", status.content, status.container, 4.5)
        }
        // Drafts are outlined, so their text sits on the surface or background.
        assertContrast("$name status.draft/surface", s.draft.content, c.surface, 4.5)
        assertContrast("$name status.draft/background", s.draft.content, c.background, 4.5)
        // "Draft saved" uses the paid colour on the editor's background.
        assertContrast("$name Draft saved/background", s.paid.content, c.background, 4.5)
    }

    @Test fun `light theme meets contrast minimums`() = checkScheme("light", LightColors)

    @Test fun `dark theme meets contrast minimums`() = checkScheme("dark", DarkColors)

    @Test fun `status colours meet contrast minimums`() {
        checkStatus("light", LightStatusColors, LightColors)
        checkStatus("dark", DarkStatusColors, DarkColors)
    }
}
