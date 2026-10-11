package dev.danielclements.puck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import dev.atvremote.protocol.mrp.NowPlaying
import dev.atvremote.protocol.mrp.PlaybackState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.SoundEffectConstants
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import dev.atvremote.protocol.companion.AppInfo
import dev.atvremote.protocol.companion.Button
import dev.atvremote.protocol.companion.TouchAcceleration
import dev.atvremote.protocol.companion.TouchPhase
import dev.atvremote.protocol.discovery.AppleTvDevice
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.hypot

// combinedClickable, for the power button's tap-versus-hold split.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteScreen(device: AppleTvDevice, state: UiState, vm: RemoteViewModel) {
    var showApps by remember { mutableStateOf(false) }
    // The system back button folds the app drawer away before it ever gets a
    // chance to leave the remote itself.
    BackHandler(enabled = showApps) { showApps = false }
    // A hold has no on-screen feedback of its own, so the buzz is the only
    // signal that it registered rather than a tap.
    val feedback = rememberFeedback(state.settings.hapticsEnabled, state.settings.buttonSoundEnabled)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Landscape puts the pad and the controls side by side; portrait stacks
        // them, with the pad taking whatever height is left over.
        val landscape = maxWidth > maxHeight

        // The drawer is the landscape screen too, so its apps load for it.
        LaunchedEffect(showApps, landscape) {
            if ((showApps || landscape) && state.apps.isEmpty()) vm.loadApps()
        }

        Column(Modifier.fillMaxSize()) {
        // ---- header (DESIGN_SPEC.md §6.1) ----
        Row(
            modifier = Modifier.height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuckRoundButton(
                onClick = { vm.disconnect() },
                size = 44.dp,
                containerColor = Puck.colors.raised,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_disconnect),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = PuckText.deviceTitle, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                val subtitle = when {
                    state.reconnecting -> stringResource(R.string.reconnecting)
                    state.capabilities?.volume == true && state.volume != null ->
                        stringResource(R.string.volume_percent, (state.volume * 100).toInt())
                    else -> stringResource(R.string.connected)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = if (state.reconnecting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    Box(
                        Modifier
                            .size(8.dp)
                            .then(
                                if (!state.reconnecting) {
                                    Modifier.drawBehind {
                                        drawCircle(dotColor.copy(alpha = 0.2f), radius = size.width * 0.9f)
                                    }
                                } else Modifier,
                            )
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(subtitle, style = PuckText.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            PuckActionTray {
                PuckTrayIconButton(onClick = { vm.toggleKeyboard() }, active = state.keyboardOpen) {
                    Icon(Icons.Default.Keyboard, contentDescription = stringResource(R.string.cd_keyboard), modifier = Modifier.size(20.dp))
                }
                PuckTrayIconButton(onClick = { showApps = !showApps }, active = showApps) {
                    Icon(Icons.Default.Apps, contentDescription = stringResource(R.string.cd_apps), modifier = Modifier.size(20.dp))
                }
                // A tap opens Control Centre, which is what the remote's own power
                // button does — sleeping outright, with no confirmation and no way
                // back, is not what pressing power on a remote means. Sleep lives
                // inside that panel; waking does not, since a sleeping TV has no
                // panel to open, so it stays on the hold. The quiet arc is only a
                // hint that holding it does something different — tap/hold itself
                // is unchanged.
                Box(
                    Modifier
                        .size(42.dp)
                        .puckHintArc(MaterialTheme.colorScheme.tertiary)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .combinedClickable(
                            onClick = { vm.holdHome() },
                            onLongClick = {
                                feedback(HapticFeedbackType.LongPress)
                                vm.wake()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.cd_power),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        state.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message) { vm.dismissError() }
        }

        AnimatedVisibility(
            visible = state.keyboardOpen,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = ExitTransition.None,
        ) {
            Spacer(Modifier.height(12.dp))
            TextEntry(device, state, vm)
        }


        // The drawer owns this screen in two cases: opened explicitly, and
        // landscape — on a big-but-short display the pad has no room to
        // breathe, and a grid of launchable apps is the better use of the
        // space. Nothing else: just the apps.
        AnimatedVisibility(
            visible = showApps || landscape,
            modifier = Modifier.weight(1f),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = ExitTransition.None,
        ) {
            AppGrid(state, vm, Modifier.fillMaxSize(), columns = if (landscape) 5 else 3)
        }

        if (!(showApps || landscape)) {
            // Every seam — above the card, around the pad, between the button
            // rows, and at the page edges — shares the leftover height
            // equally, while the pad region gets the lion's share so the
            // square inside it grows to full width whenever the space allows.
            Spacer(Modifier.weight(1f))
            NowPlayingSection(device, state, vm)
            Spacer(Modifier.weight(1f))

            // Hidden outright while typing: squeezed down to a sliver is the
            // worst of both worlds, and the pad is useless during text entry.
            if (!state.keyboardOpen) {
            Box(
                modifier = Modifier
                    .weight(10f)
                    .fillMaxWidth()
                    .animateContentSize(),
                contentAlignment = Alignment.Center,
            ) {
                TouchPad(
                    modifier = Modifier.fillMaxSize(),
                    hapticsEnabled = state.settings.hapticsEnabled,
                    soundEnabled = state.settings.buttonSoundEnabled,
                    sensitivity = state.settings.sensitivity.multiplier,
                    connected = !state.reconnecting,
                    onDirectionDown = {
                        feedback(HapticFeedbackType.LongPress)
                        vm.padDirectionDown(it)
                    },
                    onDirectionUp = { vm.padDirectionUp(it) },
                    onSelect = {
                        // OK commits the focused field: fold the panel with it.
                        if (state.keyboardOpen) vm.toggleKeyboard()
                        vm.press(Button.SELECT)
                    },
                    onSelectDown = { vm.selectDown() },
                    onSelectUp = { vm.selectUp() },
                    onTouch = { x, y, phase -> vm.touch(x, y, phase) },
                )
            }
            }

            Spacer(Modifier.weight(1f))
            Deck(state, vm)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * The now-playing card, always visible while paired. Keeping it permanent
 * (playing or not) keeps the pad's size — and with it the tap-vs-swipe
 * geometry — identical in every state, which the gesture classifier relies
 * on. When nothing plays the card simply says so.
 */
@Composable
private fun NowPlayingSection(
    device: AppleTvDevice,
    state: UiState,
    vm: RemoteViewModel,
) {
    Spacer(Modifier.height(12.dp))
    if (state.airplayPaired) NowPlayingCard(state, state.nowPlaying ?: NowPlaying(), vm)
    else EnableNowPlaying(device, state, vm)
}

/**
 * The deck: a tray holding the transport row (back/menu, play/pause with its
 * "moat" ring, home) and the volume rocker (DESIGN_SPEC.md §6.3).
 */
@Composable
private fun Deck(state: UiState, vm: RemoteViewModel) {
    val feedback = rememberFeedback(state.settings.hapticsEnabled, state.settings.buttonSoundEnabled)
    val scheme = MaterialTheme.colorScheme
    val c = Puck.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(c.tray)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PuckRoundButton(
                onClick = { feedback(HapticFeedbackType.LongPress); vm.press(Button.MENU) },
                size = 68.dp,
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.menu),
                    modifier = Modifier.size(26.dp),
                )
            }

            // One button toggles playback, so it shows the action it will
            // perform. Without now-playing there is no state to reflect, and
            // it falls back to the play glyph.
            val isPlaying = state.nowPlaying?.playbackState == PlaybackState.PLAYING
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(c.tray)
                    .border(1.dp, scheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PuckRoundButton(
                    onClick = { feedback(HapticFeedbackType.LongPress); vm.press(Button.PLAY_PAUSE) },
                    size = 92.dp,
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            PuckRoundButton(
                onClick = { feedback(HapticFeedbackType.LongPress); vm.press(Button.HOME) },
                size = 68.dp,
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home), modifier = Modifier.size(26.dp))
            }
        }

        PuckVolumeRocker(
            onDown = { feedback(HapticFeedbackType.LongPress); vm.volumeDown() },
            onUp = { feedback(HapticFeedbackType.LongPress); vm.volumeUp() },
            down = {
                Icon(Icons.Default.VolumeDown, contentDescription = stringResource(R.string.volume_down), modifier = Modifier.size(24.dp))
            },
            up = {
                Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.volume_up), modifier = Modifier.size(24.dp))
            },
        )
    }
}

/**
 * The Siri Remote's click pad as one surface, and the whole surface is live.
 *
 * Styled to match the hardware remote's own glass clickpad as closely as a
 * flat screen can: plain dark glass, no printed arrows or labels. The real
 * pad carries no affordances either — its shape and the remote's physical
 * feel are the only things that teach a direction tap from a press from a
 * swipe, so this one leans on the same gesture logic to carry that meaning
 * without needing glyphs to say it.
 *
 * Gesture classification is positional:
 *  - a tap inside the centre circle selects; holding it is the long press
 *    that opens context menus
 *  - a tap on the rim steers by the dominant axis of the tap position, which
 *    gives every direction a quadrant-sized target
 *  - a drag anywhere streams real touch samples (Press, accelerated Holds,
 *    Release) so momentum scrolling behaves like the hardware remote
 *
 * tvOS repeats a held direction itself, so rim holds send the direction once.
 */
/** The real centre-vs-edge hit threshold, as a fraction of the pad's own diameter — see [TouchPad]'s gesture block. */
private const val ZONE_DIAMETER_FRACTION = 0.38f * 2f

@Composable
private fun TouchPad(
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    soundEnabled: Boolean = false,
    sensitivity: Float = 1f,
    connected: Boolean = true,
    onDirectionDown: (Button) -> Unit,
    onDirectionUp: (Button) -> Unit,
    onSelect: () -> Unit,
    onSelectDown: () -> Unit,
    onSelectUp: () -> Unit,
    onTouch: (x: Int, y: Int, phase: TouchPhase) -> Unit,
) {
    val feedback = rememberFeedback(hapticsEnabled, soundEnabled)
    val touchPadDescription = stringResource(R.string.touchpad_description)
    val scheme = MaterialTheme.colorScheme
    val puckColors = Puck.colors

    // Drawing-only state the gesture block below feeds as it runs — none of
    // it is read by the gesture logic itself, only by drawTrackpad().
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var touchTrail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var centerPressed by remember { mutableStateOf(false) }
    val ringAlpha by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = tween(400),
        label = "trackpadRing",
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        val haloColor = scheme.primary.copy(alpha = 0.08f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(side)
                .then(
                    if (connected) Modifier.border(6.dp, haloColor, RoundedCornerShape(percent = 35))
                    else Modifier,
                )
                .clip(RoundedCornerShape(percent = 35))
                .pointerInput(Unit) {
                    val centre = 500f
                    // Frac is measured against a fixed reference pad — the size
                    // the sensitivity curve was tuned on — not against this
                    // surface. The surface changes shape with the layout, and a
                    // small one would amplify finger tremor into focus jumps.
                    //
                    // 160 dp, not the phone's full touch surface: a real Siri
                    // Remote's clickpad is only ~35 mm, and even with the gain
                    // curve's amplification a reference closer to this surface's
                    // actual size (300dp) left an ordinary swipe barely crossing
                    // it once — reading on the TV as "moved one item," not a
                    // swipe. A smaller reference means the same finger travel
                    // crosses it more times, which is both more frac (more raw
                    // movement) and higher frac/sec (more acceleration gain).
                    val refPx = 160.dp.toPx()
                    // A touch more generous than the visual circle (0.30):
                    // taps near its edge are still selects, and the rim
                    // chevrons sit far enough out to stay directional.
                    val centreRadius = minOf(size.width, size.height) * 0.38f

                    fun direction(pos: Offset) {
                        feedback(HapticFeedbackType.LongPress)
                        val dx = pos.x - size.width / 2f
                        val dy = pos.y - size.height / 2f
                        val button = if (abs(dx) > abs(dy)) {
                            if (dx > 0) Button.RIGHT else Button.LEFT
                        } else {
                            if (dy > 0) Button.DOWN else Button.UP
                        }
                        onDirectionDown(button)
                        onDirectionUp(button)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val start = down.position
                        // Deliberately larger than the system touch slop: a
                        // tap whose finger drifts a few dp must not turn into
                        // a micro-swipe — in video players even a tiny
                        // horizontal swipe skips ±10 seconds, which read as
                        // mysterious rewinds on centre taps.
                        // Rim vs centre is decided on touch-down, so rim
                        // presses respond the instant the finger lands.
                        val centreDown = run {
                            val dx = start.x - size.width / 2f
                            val dy = start.y - size.height / 2f
                            hypot(dx, dy) < centreRadius
                        }

                        // Rim presses hold the HID key down, so tvOS
                        // repeats the direction for as long as the finger
                        // stays; the response is instantaneous.
                        var rimDirection: Button? = null
                        if (!centreDown) {
                            val dir = run {
                                val dx = start.x - size.width / 2f
                                val dy = start.y - size.height / 2f
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Button.RIGHT else Button.LEFT
                                } else {
                                    if (dy > 0) Button.DOWN else Button.UP
                                }
                            }
                            onDirectionDown(dir)
                            rimDirection = dir
                        }

                        var drag = false
                        var tap = false
                        var holdSelect = false
                        var upPos = start

                        var vx = centre
                        var vy = centre
                        var lastPos = start
                        var lastTime = down.uptimeMillis

                        fun advance(pos: Offset, time: Long) {
                            val dt = (time - lastTime).coerceAtLeast(1L) / 1000f
                            val fracX = (pos.x - lastPos.x) / refPx
                            val fracY = (pos.y - lastPos.y) / refPx
                            val speed = hypot(fracX, fracY) / dt
                            val gain = TouchAcceleration.gain(speed)
                            vx = (vx + fracX * 1000f * gain * HORIZONTAL_SENSITIVITY * sensitivity)
                                .coerceIn(0f, 1000f)
                            vy = (vy + fracY * 1000f * gain * VERTICAL_SENSITIVITY * sensitivity)
                                .coerceIn(0f, 1000f)
                            lastPos = pos
                            lastTime = time
                            onTouch(vx.toInt(), vy.toInt(), TouchPhase.HOLD)
                            // Drawing only: the live finger dot and its short trail.
                            touchPoint = pos
                            touchTrail = (touchTrail + pos).takeLast(8)
                        }

                        // Phase 1: for centre touches, wait for up (tap), the
                        // long-press timeout (held select), or movement (drag).
                        if (centreDown) {
                            val finished = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) { tap = true; upPos = change.position; break }
                                    if ((change.position - start).getDistance() > CENTRE_COMMIT_DP.dp.toPx()) { drag = true; break }
                                    change.consume()
                                }
                                true
                            }
                            if (finished == null) {
                                holdSelect = true
                                centerPressed = true
                                onSelectDown()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) { upPos = change.position; break }
                                    change.consume()
                                }
                                centerPressed = false
                            }
                        } else {
                            // Rim: the direction is already held down; a
                            // movement past the slop hands the gesture over to
                            // a swipe.
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUp()) { tap = true; upPos = change.position; break }
                                if ((change.position - start).getDistance() > RIM_COMMIT_DP.dp.toPx()) { drag = true; break }
                                change.consume()
                            }
                        }

                        if (drag) {
                            if (rimDirection != null) onDirectionUp(rimDirection)
                            onTouch(500, 500, TouchPhase.PRESS)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                advance(change.position, change.uptimeMillis)
                                if (change.changedToUp()) break
                                change.consume()
                            }
                            onTouch(vx.toInt(), vy.toInt(), TouchPhase.RELEASE)
                            touchPoint = null
                            touchTrail = emptyList()
                        } else {
                            when {
                                holdSelect -> onSelectUp()
                                centreDown && tap -> {
                                    // Classify by the midpoint of the gesture:
                                    // a tap that drifts a little is still a
                                    // select unless it clearly left the centre.
                                    val mid = Offset(
                                        (start.x + upPos.x) / 2f,
                                        (start.y + upPos.y) / 2f,
                                    )
                                    val dx = mid.x - size.width / 2f
                                    val dy = mid.y - size.height / 2f
                                    if (hypot(dx, dy) < centreRadius) onSelect()
                                    else direction(mid)
                                }
                                else -> onDirectionUp(rimDirection ?: return@awaitEachGesture)
                            }
                        }
                    }
                }
                .semantics {
                    contentDescription = touchPadDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            // Deliberately no printed arrows or "OK" — the hardware clickpad
            // carries no labels either, and its shape and the gesture
            // behaviour alone teach a tap from a hold from a swipe. The
            // "disc system" drawing layer below (well, live ring, edge
            // ticks, zone line, dimple, touch feedback) is purely tonal —
            // see PuckDepth.kt's drawTrackpad(). TalkBack still gets a
            // description (above), just not a visible one.
            Canvas(Modifier.fillMaxSize()) {
                drawTrackpad(
                    s = TrackpadVisualState(
                        connected = connected,
                        ringAlpha = ringAlpha,
                        touch = touchPoint,
                        trail = touchTrail,
                        centerPressed = centerPressed,
                        zoneDiameterFraction = ZONE_DIAMETER_FRACTION,
                    ),
                    scheme = scheme,
                    c = puckColors,
                    corner = side * 0.35f,
                )
            }
        }
    }
}

/**
 * Fire [onStep] once on touch-down, then keep repeating while the finger stays
 * down — first repeat after [firstDelayMs], then every [stepDelayMs]. How the
 * volume keys step without being tapped over and over.
 */
fun Modifier.repeatOnHold(
    firstDelayMs: Long = 400,
    stepDelayMs: Long = 150,
    onPressedChange: (Boolean) -> Unit = {},
    onStep: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        onPressedChange(true)
        onStep()
        var steps = 1
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                val heldFor = (change.uptimeMillis - down.uptimeMillis - firstDelayMs)
                    .coerceAtLeast(0L)
                val target = 1 + (heldFor / stepDelayMs).toInt()
                while (steps < target) {
                    onStep()
                    steps++
                }
                if (!change.pressed) break
                change.consume()
            }
        } finally {
            onPressedChange(false)
        }
    }
}

// Both axes 1:1 against the reference pad, so the same finger travel moves
// the focus the same distance whichever way you swipe.
private const val HORIZONTAL_SENSITIVITY = 1.0f
private const val VERTICAL_SENSITIVITY = 1.0f

/**
 * A haptic buzz, plus an optional system click sound, for a discrete button
 * press — gated by the user's Settings toggles rather than always firing.
 *
 * Every call site asks for [HapticFeedbackType.LongPress], not the seemingly
 * more fitting [HapticFeedbackType.TextHandleMove] — it maps to
 * `HapticFeedbackConstants.TEXT_HANDLE_MOVE`, which several OEMs (Samsung's
 * One UI among them) silently no-op, since it's meant for a text cursor
 * being dragged, not a generic button tap. `LONG_PRESS` is the one that
 * reliably fires everywhere, Compose's haptic API just doesn't expose
 * anything named for the occasion.
 */
@Composable
private fun rememberFeedback(hapticsEnabled: Boolean, soundEnabled: Boolean): (HapticFeedbackType) -> Unit {
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(hapticsEnabled, soundEnabled, haptics, view) {
        { type: HapticFeedbackType ->
            if (hapticsEnabled) haptics.performHapticFeedback(type)
            if (soundEnabled) view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}

/**
 * How far the finger must travel before a gesture commits as a swipe. Below
 * it everything is a tap — the gate that keeps tap drift from becoming a
 * micro-swipe (which video players read as a +-10 second skip).
 */
private const val RIM_COMMIT_DP = 40

/**
 * Centre taps get a larger allowance: the pad shrinks while the now-playing
 * card is up, and a tap near the small circle's edge must still be a select —
 * a rim misread there plays as a 10 second skip in the player.
 */
private const val CENTRE_COMMIT_DP = 60

/**
 * Text entry for the focused field on the Apple TV.
 *
 * Text is pushed on submit rather than per keystroke: each send restarts the
 * RTI session to read authoritative state, so per-character sends would be both
 * slow and racy.
 */
@Composable
private fun TextEntry(device: AppleTvDevice, state: UiState, vm: RemoteViewModel) {
    var draft by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme
    val c = Puck.colors

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Styled per DESIGN_SPEC.md §6.5's panel (raised fill, well field with a
    // primary border, a primary send puck) but kept as the existing inline
    // expanding panel rather than turned into a real bottom sheet overlay —
    // that would change whether the trackpad stays mounted underneath it,
    // which is layout behavior, not presentation.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.raised)
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.keyboard_panel_label, device.name.uppercase()),
            style = PuckText.label,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text(stringResource(R.string.type_on_tv)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.well,
                    unfocusedContainerColor = c.well,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outlineVariant,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    vm.sendText(draft)
                    draft = ""
                }),
            )
            Spacer(Modifier.width(10.dp))
            PuckRoundButton(
                onClick = { vm.sendText(draft); draft = "" },
                size = 56.dp,
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
            ) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send_text), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            PuckRoundButton(
                onClick = { vm.clearText() },
                size = 56.dp,
                containerColor = c.card,
                contentColor = scheme.onSurfaceVariant,
            ) {
                Icon(Icons.Default.Backspace, contentDescription = stringResource(R.string.clear_field), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        val status = when {
            state.checkingField -> stringResource(R.string.checking_field)
            state.fieldText == null -> stringResource(R.string.no_field)
            state.fieldText.isEmpty() -> stringResource(R.string.field_empty)
            else -> stringResource(R.string.field_is, state.fieldText)
        }
        Text(status, style = PuckText.caption, color = scheme.onSurfaceVariant)
    }
}

/**
 * Now-playing card, modelled on the iOS Control Centre widget: artwork and
 * track on top, a scrubber, transport controls, then volume.
 *
 * Artwork arrives as raw bytes on the MRP channel; it is decoded here rather
 * than in the protocol layer so that module stays free of Android types.
 */
@Composable
private fun NowPlayingCard(state: UiState, playing: NowPlaying, vm: RemoteViewModel) {
    val artwork = remember(playing.artwork?.size) {
        playing.artwork?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }

    // The device reports a position now and then, so the card runs the clock
    // itself in between; the anchor it extrapolates from lives in the state.
    var position by remember { mutableStateOf(state.positionNow()) }
    LaunchedEffect(state.playhead, state.playheadAt, state.playheadAdvancing) {
        position = state.positionNow()
        while (state.playheadAdvancing) {
            delay(500)
            position = state.positionNow()
        }
    }

    val scheme = MaterialTheme.colorScheme
    val c = Puck.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Fixed height: the card must occupy the same space whether it is
            // empty or full — the pad's touch geometry depends on it.
            .height(188.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.card),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artwork != null) {
                        Image(
                            bitmap = artwork.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        playing.title ?: stringResource(R.string.nothing_playing),
                        style = PuckText.rowTitle,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    // The app is the source line. Playing and paused are
                    // already obvious from the transport glyph, so only the
                    // states that are not are worth spelling out.
                    val stateLabel = when (playing.playbackState) {
                        PlaybackState.SEEKING -> stringResource(R.string.state_seeking)
                        PlaybackState.STOPPED -> stringResource(R.string.state_stopped)
                        PlaybackState.INTERRUPTED -> stringResource(R.string.state_interrupted)
                        else -> null
                    }
                    val elapsedLabel = position?.let { formatTime(it) }
                    val caption = listOfNotNull(playing.appName ?: stateLabel, elapsedLabel).joinToString(" · ")
                    if (caption.isNotBlank()) {
                        Text(
                            caption,
                            style = PuckText.caption,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // New: previous/next track skip buttons, shown only while
                // connected — Now Playing being enabled at all is already
                // guaranteed by the caller (NowPlayingSection only shows this
                // card when state.airplayPaired is true).
                if (!state.reconnecting) {
                    Spacer(Modifier.width(8.dp))
                    PuckRoundButton(
                        onClick = vm::skipToPrevious,
                        size = 44.dp,
                        containerColor = c.raised,
                        contentColor = scheme.onSurface,
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.skip_previous_track), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    PuckRoundButton(
                        onClick = vm::skipToNext,
                        size = 44.dp,
                        containerColor = c.raised,
                        contentColor = scheme.onSurface,
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.skip_next_track), modifier = Modifier.size(20.dp))
                    }
                }
            }

            val elapsed = position
            val duration = playing.position?.second
            if (elapsed != null && duration != null) {
                Spacer(Modifier.height(12.dp))
                Scrubber(elapsed, duration) { vm.seekTo(it) }
            }

            // Transport stays available while paused: skipping about is as
            // useful then as it is during playback. The play/pause button
            // itself lives in the deck, so it is not repeated here.
            val seekable = playing.playbackState in setOf(
                PlaybackState.PLAYING, PlaybackState.PAUSED, PlaybackState.SEEKING,
            )
            if (seekable) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransportButton(Icons.Default.Replay10, stringResource(R.string.skip_back), 30.dp) {
                        vm.skip(-SKIP_SECONDS)
                    }
                    TransportButton(Icons.Default.Forward10, stringResource(R.string.skip_forward), 30.dp) {
                        vm.skip(SKIP_SECONDS)
                    }
                }
            }
        }

        // Progress bar along the bottom edge (DESIGN_SPEC.md §6.4).
        val duration = playing.position?.second
        if (duration != null && duration > 0) {
            val fraction = ((position ?: 0.0) / duration).toFloat().coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(scheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(scheme.tertiary),
                )
            }
        }
    }
}

private const val SKIP_SECONDS = 10.0

/**
 * Playhead with elapsed and remaining times.
 *
 * Dragging only moves the local fill; the seek is sent on release, because
 * every intermediate position would otherwise become its own network command.
 */
@Composable
private fun Scrubber(position: Double, duration: Double, onSeek: (Double) -> Unit) {
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubbing ?: (position / duration).toFloat().coerceIn(0f, 1f)
    val shown = fraction * duration

    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val fillColor = MaterialTheme.colorScheme.tertiary

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatTime(shown),
                style = PuckText.data,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "-${formatTime(duration - shown)}",
                style = PuckText.data,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f) * duration)
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { scrubbing = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            scrubbing?.let { onSeek(it * duration) }
                            scrubbing = null
                        },
                        onDragCancel = { scrubbing = null },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scrubbing = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(color = trackColor, cornerRadius = radius)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = fillColor,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    val hours = total / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, total % 3600 / 60, total % 60)
    else "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .size(size),
    )
}

@Composable
private fun EnableNowPlaying(
    device: dev.atvremote.protocol.discovery.AppleTvDevice,
    state: UiState,
    vm: RemoteViewModel,
) {
    val scheme = MaterialTheme.colorScheme
    val outline = scheme.outlineVariant
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .clickable(enabled = !state.busy) { vm.startAirPlayPairing(device) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.show_playing),
            style = PuckText.body.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (state.busy) "…" else stringResource(R.string.set_up),
            style = PuckText.body.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.primary,
        )
    }
}

@Composable
private fun AppGrid(state: UiState, vm: RemoteViewModel, modifier: Modifier = Modifier, columns: Int = 3) {
    if (state.apps.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text(
                stringResource(R.string.loading_apps),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(state.apps, key = { it.bundleId }) { app ->
            AppTile(app) { vm.launchApp(app.bundleId) }
        }
    }
}

/**
 * One app, drawn as a tvOS-shaped tile.
 *
 * Real artwork is fetched in the background and takes a moment to arrive, so
 * every tile starts as initials over a colour derived from its bundle id. That
 * stand-in is also the permanent answer for anything the store does not know,
 * which covers the built-in apps.
 */
@Composable
private fun AppTile(app: AppInfo, onClick: () -> Unit) {
    // tvOS icons are landscape, 400x240, and the shelf reads wrong at 1:1.
    val hue = (app.bundleId.hashCode().absoluteValue % 360).toFloat()
    val tile = Brush.linearGradient(
        listOf(Color.hsv(hue, 0.45f, 0.62f), Color.hsv((hue + 26f) % 360f, 0.55f, 0.42f))
    )

    // Apple's own apps are drawn from bundled artwork and never fetched; they
    // are in no catalogue, and the tile is the same 5:3 either way.
    val bundled = remember(app.bundleId) { appleIcon(app.bundleId) }
    val lookupId = remember(app.bundleId) { storeBundleId(app.bundleId) }
    val context = LocalContext.current
    var artwork by remember(app.bundleId) { mutableStateOf<AppArtwork?>(null) }
    LaunchedEffect(app.bundleId) {
        if (lookupId != null) artwork = AppIcons.load(context, app.name, lookupId)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(tile)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val icon = artwork
            when {
                bundled != null -> Image(
                    painter = painterResource(bundled),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Fetched artwork replaces the initials once it lands.
                icon != null -> Image(
                    bitmap = icon.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = if (icon.wide) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (icon.wide) Modifier.fillMaxSize()
                    else Modifier.fillMaxHeight().padding(vertical = 7.dp),
                )

                else -> Text(
                    initials(app.name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            app.name,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Up to two letters, the way a contact avatar reduces a name. */
private fun initials(name: String): String {
    val words = name.split(' ', '-', '+').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
