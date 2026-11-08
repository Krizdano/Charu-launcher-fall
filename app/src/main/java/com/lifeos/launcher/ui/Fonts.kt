package com.lifeos.launcher.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Playfair Display (headings) and DM Sans (UI) are bundled in app/src/main/assets/fonts/.
 * The GitHub workflow downloads them at build time; you can also commit the files yourself as
 *   PlayfairDisplay.ttf  and  DMSans.ttf  (variable fonts).
 * If a file is missing the app falls back to the system serif / sans so it never crashes.
 */
object Fonts {
    var display: FontFamily = FontFamily.Serif
        private set
    var sans: FontFamily = FontFamily.SansSerif
        private set
    private var ready = false

    @OptIn(ExperimentalTextApi::class)
    fun init(context: Context) {
        if (ready) return
        ready = true
        val am = context.assets
        val files = try { am.list("fonts")?.toSet() ?: emptySet() } catch (_: Exception) { emptySet() }

        fun load(file: String, weights: List<Int>): FontFamily? {
            if (file !in files) return null
            return try {
                Typeface.createFromAsset(am, "fonts/$file") // throws if the file is not a valid font
                FontFamily(weights.map { w -> Font("fonts/$file", am, FontWeight(w)) })
            } catch (_: Exception) {
                null
            }
        }
        load("PlayfairDisplay.ttf", listOf(400, 500, 600, 700))?.let { display = it }
        load("DMSans.ttf", listOf(300, 400, 500, 600, 700))?.let { sans = it }
    }
}
