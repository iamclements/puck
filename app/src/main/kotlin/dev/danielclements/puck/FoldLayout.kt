package dev.danielclements.puck

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.map

/** The remote screen's layout modes (DESIGN_SPEC.md §8). */
enum class RemoteLayoutMode { PHONE, CENTERED, BOOK, TABLETOP, LANDSCAPE }

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * The current folding feature, or null on a non-foldable device or one in a
 * flat posture with nothing to report. Collected from [WindowInfoTracker] —
 * the composable recomposes whenever the posture changes, which is the only
 * hook [RemoteScreen] needs to pick a new [RemoteLayoutMode].
 */
@Composable
fun rememberFoldingFeature(): FoldingFeature? {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state = produceState<FoldingFeature?>(initialValue = null, activity) {
        if (activity == null) return@produceState
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .map { info: WindowLayoutInfo -> info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
            .collect { value = it }
    }
    return state.value
}

/**
 * Picks the remote screen's layout mode from the current posture, window
 * size and orientation — DESIGN_SPEC.md §8's table, in order.
 */
fun computeLayoutMode(
    fold: FoldingFeature?,
    widthDp: Dp,
    heightDp: Dp,
): RemoteLayoutMode {
    val landscape = widthDp > heightDp
    return when {
        fold != null && fold.state == FoldingFeature.State.HALF_OPENED &&
            fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> RemoteLayoutMode.TABLETOP

        fold != null && fold.orientation == FoldingFeature.Orientation.VERTICAL && fold.isSeparating ->
            RemoteLayoutMode.BOOK

        widthDp >= 840.dp -> RemoteLayoutMode.BOOK

        widthDp >= 600.dp && widthDp < 840.dp && (fold == null || !fold.isSeparating) -> RemoteLayoutMode.CENTERED

        landscape -> RemoteLayoutMode.LANDSCAPE

        else -> RemoteLayoutMode.PHONE
    }
}
