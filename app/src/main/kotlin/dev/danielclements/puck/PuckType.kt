package dev.danielclements.puck

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Bundled as variable TTFs (res/font/) rather than downloadable Google Fonts:
// the app is used offline on a LAN, so a font that needs a network fetch on
// first use isn't an option. See THIRD_PARTY_NOTICES.md for licences.
@OptIn(ExperimentalTextApi::class)
private fun vf(res: Int, w: Int) = Font(
    res, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Bricolage = FontFamily(vf(R.font.bricolage_grotesque, 700), vf(R.font.bricolage_grotesque, 800))
val Instrument = FontFamily(
    vf(R.font.instrument_sans, 400), vf(R.font.instrument_sans, 500),
    vf(R.font.instrument_sans, 600), vf(R.font.instrument_sans, 700),
)
val PuckMono = FontFamily(vf(R.font.jetbrains_mono, 400), vf(R.font.jetbrains_mono, 700))

object PuckText {
    val wordmark = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-0.035).em, lineHeight = 40.sp)
    val screenTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.025).em)
    val screenTitleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.025).em)
    val deviceTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.015).em, lineHeight = 24.sp)
    val dialogTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.02).em)
    val rowTitle = TextStyle(fontFamily = Instrument, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val body = TextStyle(fontFamily = Instrument, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)
    val caption = TextStyle(fontFamily = Instrument, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val label = TextStyle(fontFamily = Instrument, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em)
    val data = TextStyle(fontFamily = PuckMono, fontWeight = FontWeight.Normal, fontSize = 11.5.sp)
    val pin = TextStyle(fontFamily = PuckMono, fontWeight = FontWeight.Bold, fontSize = 38.sp)
}

val PuckTypography = Typography(
    displaySmall = PuckText.wordmark,
    headlineMedium = PuckText.screenTitle,
    titleLarge = PuckText.deviceTitle,
    titleMedium = PuckText.rowTitle,
    bodyMedium = PuckText.body,
    bodySmall = PuckText.caption,
    labelSmall = PuckText.label,
)
