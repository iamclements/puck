package dev.danielclements.puck

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RemoteViews can only inflate a fixed set of view classes. Anything else (a
 * plain `<View>`, a `Space`, a ConstraintLayout) makes the launcher show
 * "Can't load widget", and nothing in the build notices. This reads the widget
 * layouts and rejects such tags.
 */
class WidgetLayoutXmlTest {

    // The list in RemoteViews' documentation.
    private val allowed = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "ImageView", "ImageButton", "Button", "ProgressBar", "Chronometer",
        "AnalogClock", "TextClock", "ViewFlipper", "ListView", "GridView", "StackView",
        "AdapterViewFlipper", "ViewStub", "CheckBox", "RadioButton", "Switch",
    )

    private fun layoutDir(): File =
        listOf("src/main/res/layout", "app/src/main/res/layout").map(::File).first { it.isDirectory }

    @Test fun widgetLayoutsUseOnlyRemoteViewsClasses() {
        val files = layoutDir().listFiles { f -> f.name.startsWith("widget_") && f.extension == "xml" }!!
        assertTrue("no widget layouts found", files.isNotEmpty())
        val bad = files.flatMap { file ->
            Regex("<([A-Za-z.]+)[\\s/>]").findAll(file.readText()).map { it.groupValues[1] }
                .filter { it !in allowed }.map { "${file.name}: <$it>" }.toList()
        }
        assertTrue("not allowed in a widget: $bad", bad.isEmpty())
    }
}
