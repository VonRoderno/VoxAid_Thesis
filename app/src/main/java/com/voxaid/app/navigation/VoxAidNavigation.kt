package com.voxaid.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voxaid.feature.main.category.CategoryScreen
import com.voxaid.feature.main.loading.LoadingScreen
import com.voxaid.feature.main.menu.MainMenuScreen

/**
 * Navigation routes for VoxAid app.
 * Using sealed class for type-safe navigation.
 */
sealed class VoxAidRoute(val route: String) {
    data object Loading : VoxAidRoute("loading")
    data object MainMenu : VoxAidRoute("main_menu")
    data object Category : VoxAidRoute("category/{mode}") {
        fun createRoute(mode: String) = "category/$mode"

        const val ARG_MODE = "mode"
    }
    data object ProtocolVariant : VoxAidRoute("variant/{mode}/{protocol}") {
        fun createRoute(mode: String, protocol: String) = "variant/$mode/$protocol"

        const val ARG_MODE = "mode"
        const val ARG_PROTOCOL = "protocol"
    }
    data object Instruction : VoxAidRoute("instruction/{mode}/{variant}") {
        fun createRoute(mode: String, variant: String) = "instruction/$mode/$variant"

        const val ARG_MODE = "mode"
        const val ARG_VARIANT = "variant"
    }
}

/**
 * Main navigation host for VoxAid.
 * Defines the navigation graph and screen transitions.
 */
@Composable
fun VoxAidNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = VoxAidRoute.Loading.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Loading screen with update check
        composable(VoxAidRoute.Loading.route) {
            LoadingScreen(
                onNavigateToMenu = {
                    navController.navigate(VoxAidRoute.MainMenu.route) {
                        // Clear loading from back stack
                        popUpTo(VoxAidRoute.Loading.route) { inclusive = true }
                    }
                }
            )
        }

        // Main menu - choose Instructional or Emergency mode
        composable(VoxAidRoute.MainMenu.route) {
            MainMenuScreen(
                onModeSelected = { mode ->
                    navController.navigate(VoxAidRoute.Category.createRoute(mode))
                }
            )
        }

        // Category selection screen
        composable(
            route = VoxAidRoute.Category.route,
            arguments = listOf(
                navArgument(VoxAidRoute.Category.ARG_MODE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString(VoxAidRoute.Category.ARG_MODE) ?: "instructional"

            CategoryScreen(
                mode = mode,
                onProtocolSelected = { protocol ->
                    navController.navigate(
                        VoxAidRoute.ProtocolVariant.createRoute(mode, protocol)
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Protocol variant selection screen
        composable(
            route = VoxAidRoute.ProtocolVariant.route,
            arguments = listOf(
                navArgument(VoxAidRoute.ProtocolVariant.ARG_MODE) {
                    type = NavType.StringType
                },
                navArgument(VoxAidRoute.ProtocolVariant.ARG_PROTOCOL) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString(VoxAidRoute.ProtocolVariant.ARG_MODE) ?: "instructional"
            val protocol = backStackEntry.arguments?.getString(VoxAidRoute.ProtocolVariant.ARG_PROTOCOL) ?: "cpr"

            com.voxaid.feature.main.variant.ProtocolVariantScreen(
                protocolId = protocol,
                mode = mode,
                onVariantSelected = { variant ->
                    navController.navigate(
                        VoxAidRoute.Instruction.createRoute(mode, variant)
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToInstructional = { variant ->
                    navController.navigate(
                        VoxAidRoute.Instruction.createRoute("Instructional",variant)
                    )
                }
            )
        }

        // Instruction screen
        composable(
            route = VoxAidRoute.Instruction.route,
            arguments = listOf(
                navArgument(VoxAidRoute.Instruction.ARG_MODE) {
                    type = NavType.StringType
                },
                navArgument(VoxAidRoute.Instruction.ARG_VARIANT) {
                    type = NavType.StringType
                }
            )
        ) {
            com.voxaid.feature.instruction.InstructionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}