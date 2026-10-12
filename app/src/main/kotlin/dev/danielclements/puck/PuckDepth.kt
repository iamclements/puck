package dev.danielclements.puck

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth from tone, not drop shadows (DESIGN_SPEC.md §5 — "every control is a
 * disc you can feel"). Raised = one container tone up, a 1dp top highlight,
 * one short soft shadow; sinks toward [PuckExtendedColors.well] on press.
 * Recessed = [PuckExtendedColors.well] fill, inner top shadow, faint inner
 * outline — a surface meant to be touched into, not tapped on.
 */
fun Modifier.raised(
    shape: Shape,
    fill: Color,
    interaction: MutableInteractionSource? = null,
): Modifier = composed {
    val c = Puck.colors
    val source = interaction ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, tween(80), label = "raisedPress")
    this
        .shadow(elevation = (6f * (1f - p)).dp, shape = shape, ambientColor = c.shade, spotColor = c.shade)
        .clip(shape)
        .background(lerp(fill, c.well, 0.08f * p))
        .drawWithContent {
            drawContent()
            // 1dp top inner highlight
            drawLine(
                c.edge.copy(alpha = c.edge.alpha * (1f - p)),
                start = Offset(size.width * 0.18f, 0.75.dp.toPx()),
                end = Offset(size.width * 0.82f, 0.75.dp.toPx()),
                strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round,
            )
        }
}

/** Recessed well: well fill + inner top shadow + faint inner outline. */
fun Modifier.recessed(shape: Shape, fill: Color? = null): Modifier = composed {
    val c = Puck.colors
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    this
        .clip(shape)
        .background(fill ?: c.well)
        .border(1.dp, outline, shape)
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(0f to c.shade, 1f to Color.Transparent, endY = 8.dp.toPx()))
        }
}

/**
 * Everything the trackpad drawing layer needs (DESIGN_SPEC.md §6.2). Purely
 * presentational — the gesture handling that produces [touch]/[trail]/
 * [centerPressed] is unchanged in [TouchPad].
 */
data class TrackpadVisualState(
    val connected: Boolean,
    val ringAlpha: Float,            // animates 0f -> 1f over 400ms on connect
    val touch: Offset?,              // current pointer, null when up
    val trail: List<Offset>,         // last ~120ms of positions
    val centerPressed: Boolean,
)

/** Draws the trackpad's "disc system" layers (DESIGN_SPEC.md §6.2), back to front. */
fun DrawScope.drawTrackpad(
    s: TrackpadVisualState,
    scheme: ColorScheme,
    c: PuckExtendedColors,
    corner: Dp = 46.dp,
) {
    val r = CornerRadius(corner.toPx())
    val primary = scheme.primary
    val ov = scheme.outlineVariant

    // 1 well + inner top shadow
    drawRoundRect(c.well, cornerRadius = r)
    drawRoundRect(Brush.verticalGradient(0f to c.shade, 1f to Color.Transparent, endY = 14.dp.toPx()), cornerRadius = r)

    // 2 ring (the outer halo is drawn by the caller, outside this surface)
    val ringColor = if (s.connected) primary.copy(alpha = 0.45f * s.ringAlpha) else ov
    drawRoundRect(ringColor, cornerRadius = r, style = Stroke(1.5.dp.toPx()))

    // 3 select feedback — the surface lifts a little while OK is held
    if (s.centerPressed) drawRoundRect(c.raised.copy(alpha = 0.5f), cornerRadius = r)

    // 4 touch feedback — only while a pointer is down
    if (s.trail.size > 1) {
        s.trail.zipWithNext().forEachIndexed { i, (a, b) ->
            val t = (i + 1f) / s.trail.size
            drawLine(primary.copy(alpha = 0.35f * t), a, b, strokeWidth = 14.dp.toPx() * t, cap = StrokeCap.Round)
        }
    }
    s.touch?.let {
        drawCircle(primary.copy(alpha = 0.2f), radius = 22.dp.toPx(), center = it)
        drawCircle(primary, radius = 7.dp.toPx(), center = it)
    }
}
