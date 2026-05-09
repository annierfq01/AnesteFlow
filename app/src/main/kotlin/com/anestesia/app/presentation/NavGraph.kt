package com.anestesia.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anestesia.app.presentation.main.MainScreen
import com.anestesia.app.presentation.vademecum.VademecumScreen

object Routes {
    const val MAIN = "main"
    const val VADEMECUM = "vademecum"
}

@Composable
fun AnestesiaNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToVademecum = { navController.navigate(Routes.VADEMECUM) }
            )
        }
        composable(Routes.VADEMECUM) {
            VademecumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
