package dev.danielclements.puck

import android.graphics.Rect
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutModeTest {

    private fun mode(w: Int, h: Int, fold: FoldingFeature? = null) =
        computeLayoutMode(fold, w.dp, h.dp)

    private fun fold(
        orientation: FoldingFeature.Orientation,
        state: FoldingFeature.State,
        separating: Boolean = true,
    ) = object : FoldingFeature {
        override val bounds = Rect()
        override val isSeparating = separating
        override val occlusionType = FoldingFeature.OcclusionType.NONE
        override val orientation = orientation
        override val state = state
    }

    @Test fun `phone portrait`() = assertEquals(RemoteLayoutMode.PHONE, mode(411, 891))

    @Test fun `phone on its side is compact landscape, not book`() {
        assertEquals(RemoteLayoutMode.LANDSCAPE, mode(914, 411))
        assertEquals(RemoteLayoutMode.LANDSCAPE, mode(686, 309))
        assertEquals(RemoteLayoutMode.LANDSCAPE, mode(800, 380))
    }

    @Test fun `600 to 839 wide and tall is centered`() = assertEquals(RemoteLayoutMode.CENTERED, mode(640, 900))

    @Test fun `unfolded foldable and tablets are book`() {
        assertEquals(RemoteLayoutMode.BOOK, mode(841, 701))
        assertEquals(RemoteLayoutMode.BOOK, mode(1280, 800))
    }

    @Test fun `vertical separating hinge is book even when narrow`() {
        val f = fold(FoldingFeature.Orientation.VERTICAL, FoldingFeature.State.FLAT)
        assertEquals(RemoteLayoutMode.BOOK, mode(700, 900, f))
    }

    @Test fun `half open horizontal hinge is tabletop`() {
        val f = fold(FoldingFeature.Orientation.HORIZONTAL, FoldingFeature.State.HALF_OPENED)
        assertEquals(RemoteLayoutMode.TABLETOP, mode(700, 900, f))
    }

    @Test fun `a flat horizontal separating hinge is not tabletop`() {
        val f = fold(FoldingFeature.Orientation.HORIZONTAL, FoldingFeature.State.FLAT)
        assertEquals(RemoteLayoutMode.PHONE, mode(640, 900, f))
    }

    @Test fun `a non-separating fold does not stop centered`() {
        val f = fold(FoldingFeature.Orientation.HORIZONTAL, FoldingFeature.State.FLAT, separating = false)
        assertEquals(RemoteLayoutMode.CENTERED, mode(640, 900, f))
    }
}
