package io.github.aedev.flow.ui.focus

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithContent
import io.github.aedev.flow.data.focus.FocusSettings

/** The focus options, provided once at the root of the app. */
val LocalFocus = staticCompositionLocalOf { FocusSettings() }

/** Draws everything below in black and white. */
fun Modifier.blackAndWhite(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        drawWithContent {
            val paint =
                Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                drawContent()
                canvas.restore()
            }
        }
    }
