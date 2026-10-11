package dev.danielclements.puck

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A round "puck" button: [Modifier.raised] plus a click target. Used for the
 * header back button, transport pucks, skip buttons, the send button — every
 * circular control in DESIGN_SPEC.md §6.
 */
@Composable
fun PuckRoundButton(
    onClick: () -> Unit,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .raised(shape, containerColor, interaction)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * The recessed pill that holds the header's keyboard/apps/power buttons, and
 * the landscape rail's keyboard/power pair (DESIGN_SPEC.md §6.1).
 */
@Composable
fun PuckActionTray(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .recessed(shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A flat icon button inside a [PuckActionTray] — keyboard, apps. */
@Composable
fun PuckTrayIconButton(
    onClick: () -> Unit,
    size: Dp = 42.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (active) MaterialTheme.colorScheme.primary else contentColor,
            content = content,
        )
    }
}

/**
 * A short 2dp arc hugging a disc from just outside its edge — the power
 * button's quiet "holding does something different" hint (DESIGN_SPEC.md
 * §6.1). A drawing-only modifier: the button underneath keeps whatever
 * click/long-click handling it already has.
 */
fun Modifier.puckHintArc(
    color: Color,
    startAngleDegrees: Float = 35f,
    sweepAngleDegrees: Float = 90f,
    inset: Dp = 3.dp,
    strokeWidth: Dp = 2.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val insetPx = inset.toPx()
    val strokePx = strokeWidth.toPx()
    val topLeft = Offset(-insetPx, -insetPx)
    val arcSize = Size(size.width + insetPx * 2, size.height + insetPx * 2)
    drawArc(
        color = color,
        startAngle = startAngleDegrees,
        sweepAngle = sweepAngleDegrees,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(strokePx, cap = StrokeCap.Round),
    )
}

/** A 60dp volume rocker pill: − / VOL / + as three independent buttons. */
@Composable
fun PuckVolumeRocker(
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
    down: @Composable () -> Unit,
    up: @Composable () -> Unit,
) {
    val c = Puck.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .raised(RoundedCornerShape(height / 2), c.raised),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PuckRockerHalf(onDown, Modifier.weight(1f).fillMaxHeight(), down)
        Text("VOL", style = PuckText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PuckRockerHalf(onUp, Modifier.weight(1f).fillMaxHeight(), up)
    }
}

@Composable
private fun PuckRockerHalf(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface, content = content)
    }
}

/** Switch matching DESIGN_SPEC.md §6.6: 58×34 track, 26dp thumb, 150ms spring. */
@Composable
fun PuckSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val c = Puck.colors
    val trackColor = if (checked) scheme.primary else c.well
    val thumbColor = if (checked) scheme.onPrimary else scheme.onSurfaceVariant
    val interaction = remember { MutableInteractionSource() }
    val offset by animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "switchThumb",
    )
    Box(
        modifier
            .width(58.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(trackColor)
            .then(
                if (!checked) Modifier.border(2.dp, scheme.outlineVariant, RoundedCornerShape(17.dp)) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 4.dp)
                .size(26.dp)
                .raised(CircleShape, thumbColor),
        )
    }
}

/** The 3-segment Low/Medium/High-style picker (DESIGN_SPEC.md §6.6). */
@Composable
fun <T> PuckSegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    icon: (@Composable (T, Boolean) -> Unit)? = null,
) {
    val c = Puck.colors
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .recessed(RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (isSelected) Modifier.raised(shape, c.raised, interaction) else Modifier.clip(shape))
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .then(
                        if (isSelected) {
                            // A 2dp primary underline inset from the puck's own bottom edge.
                            Modifier.drawWithContent {
                                drawContent()
                                val inset = 10.dp.toPx()
                                drawLine(
                                    scheme.primary,
                                    start = Offset(inset, size.height - 2.dp.toPx()),
                                    end = Offset(size.width - inset, size.height - 2.dp.toPx()),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        } else Modifier,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                ) {
                    icon?.invoke(option, isSelected)
                    if (icon != null) Box(Modifier.width(8.dp))
                    Text(label(option), style = PuckText.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                }
            }
        }
    }
}

/**
 * The custom bottom-anchored dialog card (DESIGN_SPEC.md §6.6): 12dp margin,
 * 36dp radius, [PuckExtendedColors.raised] fill, no default Material dialog
 * chrome.
 */
@Composable
fun PuckDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val c = Puck.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(c.raised)
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 20.dp),
            ) {
                content()
            }
        }
    }
}
