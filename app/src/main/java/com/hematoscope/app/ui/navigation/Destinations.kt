package com.hematoscope.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes in the app. */
object Routes {
    const val HOME = "home"
    const val ATLAS = "atlas"
    const val CAPTURE = "capture"
    const val DIFFERENTIAL = "differential"
    const val MEASUREMENT = "measurement"
    const val COMPARISON = "comparison"
    const val CASES = "cases"
    const val CALIBRATION = "calibration"
}

/** Primary tabs shown in the bottom navigation bar. */
enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Inicio", Icons.Outlined.Home),
    ATLAS(Routes.ATLAS, "Atlas", Icons.Outlined.MenuBook),
    CAPTURE(Routes.CAPTURE, "Captura", Icons.Outlined.CameraAlt),
    DIFFERENTIAL(Routes.DIFFERENTIAL, "Recuento", Icons.Outlined.Bloodtype),
    MEASUREMENT(Routes.MEASUREMENT, "Medición", Icons.Outlined.Straighten);

    companion object {
        val routes = entries.map { it.route }.toSet()
    }
}
