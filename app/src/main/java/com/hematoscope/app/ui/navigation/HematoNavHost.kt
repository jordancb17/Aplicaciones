package com.hematoscope.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hematoscope.app.ui.screens.atlas.AtlasScreen
import com.hematoscope.app.ui.screens.calibration.CalibrationScreen
import com.hematoscope.app.ui.screens.capture.CaptureScreen
import com.hematoscope.app.ui.screens.cases.CasesScreen
import com.hematoscope.app.ui.screens.comparison.ComparisonScreen
import com.hematoscope.app.ui.screens.differential.DifferentialScreen
import com.hematoscope.app.ui.screens.home.HomeScreen
import com.hematoscope.app.ui.screens.measurement.MeasurementScreen

@Composable
fun HematoNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Only show the bar on top-level tabs.
            if (currentRoute in TopLevelTab.routes) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.ATLAS) { AtlasScreen() }
            composable(Routes.CAPTURE) { CaptureScreen() }
            composable(Routes.DIFFERENTIAL) { DifferentialScreen() }
            composable(Routes.MEASUREMENT) { MeasurementScreen() }
            composable(Routes.COMPARISON) { ComparisonScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CASES) { CasesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CALIBRATION) { CalibrationScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
