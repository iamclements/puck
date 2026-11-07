package dev.danielclements.puck

import dev.atvremote.protocol.companion.Button

/** What a widget button can tell an Apple TV to do without opening the app. */
enum class WidgetCommand(val id: String, val button: Button) {
    PLAY_PAUSE("play_pause", Button.PLAY_PAUSE),
    VOLUME_UP("vol_up", Button.VOLUME_UP),
    VOLUME_DOWN("vol_down", Button.VOLUME_DOWN),
    SLEEP("sleep", Button.SLEEP),
    WAKE("wake", Button.WAKE);

    val labelRes: Int
        get() = when (this) {
            PLAY_PAUSE -> R.string.widget_cmd_play_pause
            VOLUME_UP -> R.string.widget_cmd_volume_up
            VOLUME_DOWN -> R.string.widget_cmd_volume_down
            SLEEP -> R.string.widget_cmd_sleep
            WAKE -> R.string.widget_cmd_wake
        }

    /** Accessibility text, taking the TV's name. */
    val descriptionRes: Int
        get() = when (this) {
            PLAY_PAUSE -> R.string.widget_cd_play_pause
            VOLUME_UP -> R.string.widget_cd_volume_up
            VOLUME_DOWN -> R.string.widget_cd_volume_down
            SLEEP -> R.string.widget_cd_sleep
            WAKE -> R.string.widget_cd_wake
        }

    companion object {
        fun fromId(id: String?): WidgetCommand? = entries.firstOrNull { it.id == id }
    }
}

/** Marketing names for the model identifiers an Apple TV advertises. */
object AppleTvModels {
    fun friendly(model: String?): String = when (model) {
        "AppleTV5,3" -> "Apple TV HD"
        "AppleTV6,2" -> "Apple TV 4K"
        "AppleTV11,1" -> "Apple TV 4K (2nd gen)"
        "AppleTV14,1" -> "Apple TV 4K (3rd gen)"
        else -> "Apple TV"
    }
}
