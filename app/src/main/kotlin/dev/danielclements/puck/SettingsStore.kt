package dev.danielclements.puck

import android.content.Context

/** How strongly a drag on the touch pad translates into on-screen movement. */
enum class TouchSensitivity(val multiplier: Float) {
    LOW(0.8f),
    MEDIUM(1.2f),
    HIGH(1.8f),
}

/** Which side the controls sit on when the screen is wide enough for two panes. */
enum class ControlsSide { LEFT, RIGHT }

/** Which control the remote screen's main surface is. */
enum class RemoteStyle { TRACKPAD, DPAD }

data class AppSettings(
    val hapticsEnabled: Boolean = true,
    val buttonSoundEnabled: Boolean = false,
    val sensitivity: TouchSensitivity = TouchSensitivity.MEDIUM,
    val remoteStyle: RemoteStyle = RemoteStyle.TRACKPAD,
    val controlsSide: ControlsSide = ControlsSide.RIGHT,
)

/** User-facing preferences, separate from [CredentialStore]'s per-device data. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        buttonSoundEnabled = prefs.getBoolean(KEY_SOUND, false),
        sensitivity = prefs.getString(KEY_SENSITIVITY, null)
            ?.let { saved -> TouchSensitivity.entries.firstOrNull { it.name == saved } }
            ?: TouchSensitivity.MEDIUM,
        remoteStyle = prefs.getString(KEY_REMOTE_STYLE, null)
            ?.let { saved -> RemoteStyle.entries.firstOrNull { it.name == saved } }
            ?: RemoteStyle.TRACKPAD,
        controlsSide = prefs.getString(KEY_CONTROLS_SIDE, null)
            ?.let { saved -> ControlsSide.entries.firstOrNull { it.name == saved } }
            ?: ControlsSide.RIGHT,
    )

    fun setHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()

    fun setButtonSound(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND, enabled).apply()

    fun setSensitivity(value: TouchSensitivity) =
        prefs.edit().putString(KEY_SENSITIVITY, value.name).apply()

    fun setRemoteStyle(value: RemoteStyle) =
        prefs.edit().putString(KEY_REMOTE_STYLE, value.name).apply()

    fun setControlsSide(value: ControlsSide) =
        prefs.edit().putString(KEY_CONTROLS_SIDE, value.name).apply()

    companion object {
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_SOUND = "button_sound_enabled"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_REMOTE_STYLE = "remote_style"
        private const val KEY_CONTROLS_SIDE = "controls_side"
    }
}
